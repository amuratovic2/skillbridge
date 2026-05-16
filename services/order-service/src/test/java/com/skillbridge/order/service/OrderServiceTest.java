package com.skillbridge.order.service;

import com.skillbridge.order.client.GigClient;
import com.skillbridge.order.config.RabbitMQConfig;
import com.skillbridge.order.dto.CreateOrderRequest;
import com.skillbridge.order.dto.GigDto;
import com.skillbridge.order.events.OrderEvent;
import com.skillbridge.order.events.OrderEventPublisher;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.repository.OrderRepository;
import com.skillbridge.order.saga.OrderPlacedEvent;
import com.skillbridge.order.saga.OrderSagaPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private GigClient gigClient;
    @Mock private OrderSagaPublisher sagaPublisher;
    @Mock private OrderEventPublisher eventPublisher;

    @InjectMocks private OrderService orderService;

    private GigDto activeGig(int gigId, int freelancerId) {
        GigDto gig = new GigDto();
        gig.setId(gigId);
        gig.setFreelancerId(freelancerId);
        gig.setCost(new BigDecimal("150.00"));
        gig.setDeliveryTime(7);
        gig.setRevisionCount(3);
        gig.setStatus("ACTIVE");
        return gig;
    }

    private Order order(long id, int clientId, int sellerId, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setClientId(clientId);
        o.setSellerId(sellerId);
        o.setGigId(1);
        o.setTotalCost(new BigDecimal("150.00"));
        o.setMaxRevisions(3);
        o.setStatus(status);
        return o;
    }

    // ── create() ────────────────────────────────────────────────────────────

    @Test
    void create_populatesOrderFromGigService() {
        when(gigClient.getGig(1)).thenReturn(activeGig(1, 5));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.create(10, 1);

        assertThat(result.getClientId()).isEqualTo(10);
        assertThat(result.getGigId()).isEqualTo(1);
        assertThat(result.getSellerId()).isEqualTo(5);
        assertThat(result.getTotalCost()).isEqualByComparingTo("150.00");
        assertThat(result.getMaxRevisions()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getDeliveryDeadline()).isNotNull();
    }

    @Test
    void create_publishesSagaAndOrderPlacedEvent() {
        when(gigClient.getGig(1)).thenReturn(activeGig(1, 5));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(99L);
            return o;
        });

        orderService.create(10, 1);

        ArgumentCaptor<OrderPlacedEvent> sagaCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(sagaPublisher).publishOrderPlaced(sagaCaptor.capture());
        assertThat(sagaCaptor.getValue().orderId()).isEqualTo(99L);
        assertThat(sagaCaptor.getValue().clientId()).isEqualTo(10);
        assertThat(sagaCaptor.getValue().sellerId()).isEqualTo(5);

        ArgumentCaptor<OrderEvent> evCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishOrderEvent(evCaptor.capture());
        assertThat(evCaptor.getValue().eventType()).isEqualTo(RabbitMQConfig.ORDER_PLACED_KEY);
        assertThat(evCaptor.getValue().clientId()).isEqualTo(10);
        assertThat(evCaptor.getValue().sellerId()).isEqualTo(5);
        assertThat(evCaptor.getValue().newStatus()).isEqualTo("PENDING");
    }

    @Test
    void create_rejectsSelfOrdering() {
        when(gigClient.getGig(1)).thenReturn(activeGig(1, 10));

        assertThatThrownBy(() -> orderService.create(10, 1))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(orderRepository, never()).save(any());
        verify(sagaPublisher, never()).publishOrderPlaced(any());
        verify(eventPublisher, never()).publishOrderEvent(any());
    }

    @Test
    void create_throws400_whenGigIsNotActive() {
        GigDto pausedGig = activeGig(1, 5);
        pausedGig.setStatus("PAUSED");
        when(gigClient.getGig(1)).thenReturn(pausedGig);

        assertThatThrownBy(() -> orderService.create(10, 1))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_propagatesResourceAccessException_whenGigServiceIsDown() {
        when(gigClient.getGig(1)).thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> orderService.create(10, 1))
            .isInstanceOf(ResourceAccessException.class);
    }

    @Test
    void create_propagates404_whenGigDoesNotExist() {
        when(gigClient.getGig(999)).thenThrow(
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Gig not found"));

        assertThatThrownBy(() -> orderService.create(10, 999))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── batchCreate() ───────────────────────────────────────────────────────

    @Test
    void batchCreate_createsMultipleOrders_usingGigServiceData() {
        when(gigClient.getGig(1)).thenReturn(activeGig(1, 5));
        when(gigClient.getGig(2)).thenReturn(activeGig(2, 8));
        when(orderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest req1 = new CreateOrderRequest(); req1.setGigId(1);
        CreateOrderRequest req2 = new CreateOrderRequest(); req2.setGigId(2);

        List<Order> result = orderService.batchCreate(10, List.of(req1, req2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSellerId()).isEqualTo(5);
        assertThat(result.get(1).getSellerId()).isEqualTo(8);
    }

    // ── updateStatus() ──────────────────────────────────────────────────────

    @Test
    void updateStatus_acceptingPendingOrder_publishesAcceptedEvent() {
        Order existing = order(1L, 10, 5, OrderStatus.PENDING);
        when(orderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(1L, 5, "FREELANCER", OrderStatus.ACCEPTED, null);

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishOrderEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RabbitMQConfig.ORDER_ACCEPTED_KEY);
        assertThat(captor.getValue().oldStatus()).isEqualTo("PENDING");
        assertThat(captor.getValue().newStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void updateStatus_cancellingPendingOrder_publishesCancelledEvent() {
        Order existing = order(7L, 10, 5, OrderStatus.PENDING);
        when(orderRepository.findWithDetailsById(7L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(7L, 10, "CLIENT", OrderStatus.CANCELLED, "Promijenio sam mišljenje");

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishOrderEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RabbitMQConfig.ORDER_CANCELLED_KEY);
        assertThat(captor.getValue().note()).isEqualTo("Promijenio sam mišljenje");
    }

    @Test
    void updateStatus_rejectsForbiddenTransition() {
        Order existing = order(7L, 10, 5, OrderStatus.COMPLETED);
        when(orderRepository.findWithDetailsById(7L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
            orderService.updateStatus(7L, 10, "CLIENT", OrderStatus.PENDING, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(eventPublisher, never()).publishOrderEvent(any());
    }

    @Test
    void updateStatus_forbidsRandomUser() {
        Order existing = order(7L, 10, 5, OrderStatus.PENDING);
        when(orderRepository.findWithDetailsById(7L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
            orderService.updateStatus(7L, 999, "CLIENT", OrderStatus.ACCEPTED, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ── requestRevision() ───────────────────────────────────────────────────

    @Test
    void requestRevision_incrementsCounterAndPublishesEvent() {
        Order existing = order(7L, 10, 5, OrderStatus.DELIVERED);
        existing.setUsedRevisions(1);
        when(orderRepository.findWithDetailsById(7L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.requestRevision(7L, 10, "Pls smaller logo");

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.REVISION_REQUESTED);
        assertThat(updated.getUsedRevisions()).isEqualTo(2);

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishOrderEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RabbitMQConfig.ORDER_REVISION_REQUESTED_KEY);
        assertThat(captor.getValue().note()).isEqualTo("Pls smaller logo");
    }

    @Test
    void requestRevision_rejectsNonClient() {
        Order existing = order(7L, 10, 5, OrderStatus.DELIVERED);
        when(orderRepository.findWithDetailsById(7L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> orderService.requestRevision(7L, 5, "no"))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void requestRevision_rejectsWhenLimitReached() {
        Order existing = order(7L, 10, 5, OrderStatus.DELIVERED);
        existing.setUsedRevisions(3);
        when(orderRepository.findWithDetailsById(7L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> orderService.requestRevision(7L, 10, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
