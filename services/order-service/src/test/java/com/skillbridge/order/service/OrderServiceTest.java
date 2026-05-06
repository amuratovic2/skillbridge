package com.skillbridge.order.service;

import com.skillbridge.order.client.GigClient;
import com.skillbridge.order.dto.CreateOrderRequest;
import com.skillbridge.order.dto.GigDto;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.repository.OrderRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GigClient gigClient;

    @InjectMocks
    private OrderService orderService;

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

    private Order savedOrder(int clientId, int gigId, int sellerId) {
        Order o = new Order();
        o.setId(1L);
        o.setClientId(clientId);
        o.setGigId(gigId);
        o.setSellerId(sellerId);
        o.setTotalCost(new BigDecimal("150.00"));
        o.setMaxRevisions(3);
        o.setStatus(OrderStatus.PENDING);
        return o;
    }

    // --- create() tests ---

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
    void create_callsGigClientWithCorrectGigId() {
        when(gigClient.getGig(42)).thenReturn(activeGig(42, 7));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.create(10, 42);

        verify(gigClient).getGig(42);
    }

    @Test
    void create_savesOrderWithHistoryEntry() {
        when(gigClient.getGig(1)).thenReturn(activeGig(1, 5));
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        orderService.create(10, 1);

        Order saved = captor.getValue();
        assertThat(saved.getHistory()).hasSize(1);
        assertThat(saved.getHistory().get(0).getActionType()).isEqualTo("ORDER_CREATED");
        assertThat(saved.getHistory().get(0).getNewStatus()).isEqualTo("PENDING");
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
    void create_throws400_whenGigIsDraft() {
        GigDto draftGig = activeGig(1, 5);
        draftGig.setStatus("DRAFT");
        when(gigClient.getGig(1)).thenReturn(draftGig);

        assertThatThrownBy(() -> orderService.create(10, 1))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void create_propagatesResourceAccessException_whenGigServiceIsDown() {
        // GigClient intentionally does NOT catch ResourceAccessException so
        // Resilience4j's circuit breaker can count it. In production the CB
        // fallback converts it to 503; here we verify the raw propagation.
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
            .satisfies(ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- batchCreate() tests ---

    @Test
    void batchCreate_createsMultipleOrders_usingGigServiceData() {
        when(gigClient.getGig(1)).thenReturn(activeGig(1, 5));
        when(gigClient.getGig(2)).thenReturn(activeGig(2, 8));
        when(orderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderRequest req1 = new CreateOrderRequest();
        req1.setGigId(1);
        CreateOrderRequest req2 = new CreateOrderRequest();
        req2.setGigId(2);

        List<Order> result = orderService.batchCreate(10, List.of(req1, req2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSellerId()).isEqualTo(5);
        assertThat(result.get(1).getSellerId()).isEqualTo(8);
        verify(gigClient).getGig(1);
        verify(gigClient).getGig(2);
    }

    @Test
    void batchCreate_throws400_ifAnyGigIsNotActive() {
        GigDto deletedGig = activeGig(2, 8);
        deletedGig.setStatus("DELETED");
        when(gigClient.getGig(1)).thenReturn(activeGig(1, 5));
        when(gigClient.getGig(2)).thenReturn(deletedGig);

        CreateOrderRequest req1 = new CreateOrderRequest();
        req1.setGigId(1);
        CreateOrderRequest req2 = new CreateOrderRequest();
        req2.setGigId(2);

        assertThatThrownBy(() -> orderService.batchCreate(10, List.of(req1, req2)))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(orderRepository, never()).saveAll(any());
    }
}
