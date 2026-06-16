package com.skillbridge.order.service;

import com.skillbridge.order.config.RabbitMQConfig;
import com.skillbridge.order.events.DeliveryEvent;
import com.skillbridge.order.events.OrderEvent;
import com.skillbridge.order.events.OrderEventPublisher;
import com.skillbridge.order.model.Delivery;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.repository.DeliveryRepository;
import com.skillbridge.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderEventPublisher eventPublisher;
    @InjectMocks private DeliveryService service;

    private Order acceptedOrder() {
        Order o = new Order();
        o.setId(7L);
        o.setClientId(10);
        o.setSellerId(5);
        o.setGigId(1);
        o.setTotalCost(new BigDecimal("100"));
        o.setStatus(OrderStatus.IN_PROGRESS);
        return o;
    }

    @Test
    void create_savesFirstDeliveryAsVersion1AndMarksOrderDelivered() {
        when(orderRepository.findById(7L)).thenReturn(Optional.of(acceptedOrder()));
        when(deliveryRepository.findByOrderIdOrderByVersionNumberDesc(7L)).thenReturn(List.of());
        when(deliveryRepository.save(any())).thenAnswer(inv -> {
            Delivery d = inv.getArgument(0);
            d.setId(101L);
            return d;
        });
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Delivery result = service.create(7L, 5, "v1", null, null);

        assertThat(result.getVersionNumber()).isEqualTo(1);
        assertThat(result.getOrder().getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void create_publishesDeliveryAndDeliveredEvents() {
        when(orderRepository.findById(7L)).thenReturn(Optional.of(acceptedOrder()));
        when(deliveryRepository.findByOrderIdOrderByVersionNumberDesc(7L)).thenReturn(List.of());
        when(deliveryRepository.save(any())).thenAnswer(inv -> {
            Delivery d = inv.getArgument(0);
            d.setId(101L);
            return d;
        });
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(7L, 5, "Initial draft", null, null);

        ArgumentCaptor<DeliveryEvent> delCaptor = ArgumentCaptor.forClass(DeliveryEvent.class);
        verify(eventPublisher).publishDeliveryEvent(delCaptor.capture());
        assertThat(delCaptor.getValue().eventType()).isEqualTo(RabbitMQConfig.DELIVERY_CREATED_KEY);
        assertThat(delCaptor.getValue().versionNumber()).isEqualTo(1);
        assertThat(delCaptor.getValue().clientId()).isEqualTo(10);

        ArgumentCaptor<OrderEvent> orderCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishOrderEvent(orderCaptor.capture());
        assertThat(orderCaptor.getValue().eventType()).isEqualTo(RabbitMQConfig.ORDER_DELIVERED_KEY);
        assertThat(orderCaptor.getValue().newStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void create_incrementsVersionWhenPreviousDeliveriesExist() {
        Delivery existing = new Delivery();
        existing.setVersionNumber(2);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(acceptedOrder()));
        when(deliveryRepository.findByOrderIdOrderByVersionNumberDesc(7L)).thenReturn(List.of(existing));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Delivery result = service.create(7L, 5, "v3", null, null);

        assertThat(result.getVersionNumber()).isEqualTo(3);
    }

    @Test
    void create_throws404WhenOrderMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, 5, "x", null, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void create_rejectsNonSeller() {
        when(orderRepository.findById(7L)).thenReturn(Optional.of(acceptedOrder()));

        assertThatThrownBy(() -> service.create(7L, 99, "x", null, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN));

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void findByOrderIdRejectsNonParticipant() {
        when(orderRepository.findById(7L)).thenReturn(Optional.of(acceptedOrder()));

        assertThatThrownBy(() -> service.findByOrderId(7L, 99, "CLIENT"))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void findByOrderIdAllowsAdmin() {
        when(orderRepository.findById(7L)).thenReturn(Optional.of(acceptedOrder()));
        when(deliveryRepository.findByOrderIdOrderByVersionNumberDesc(7L)).thenReturn(List.of());

        assertThat(service.findByOrderId(7L, 99, "ADMIN")).isEmpty();
    }
}
