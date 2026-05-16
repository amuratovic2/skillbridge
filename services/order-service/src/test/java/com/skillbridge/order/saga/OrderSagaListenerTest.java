package com.skillbridge.order.saga;

import com.rabbitmq.client.Channel;
import com.skillbridge.order.config.RabbitMQConfig;
import com.skillbridge.order.events.OrderEvent;
import com.skillbridge.order.events.OrderEventPublisher;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSagaListenerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderEventPublisher eventPublisher;
    @Mock private Channel channel;
    @InjectMocks private OrderSagaListener listener;

    private Order pending(long id) {
        Order o = new Order();
        o.setId(id);
        o.setClientId(10);
        o.setSellerId(5);
        o.setGigId(1);
        o.setTotalCost(new BigDecimal("100"));
        o.setStatus(OrderStatus.PENDING);
        return o;
    }

    @Test
    void confirmedResult_promotesOrderToAcceptedAndPublishesEvent() throws Exception {
        Order order = pending(1L);
        when(orderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));

        listener.handleSagaResult(new OrderSagaResult(1L, true, null), channel, 42L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishOrderEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RabbitMQConfig.ORDER_ACCEPTED_KEY);
        assertThat(captor.getValue().oldStatus()).isEqualTo("PENDING");
        assertThat(captor.getValue().newStatus()).isEqualTo("ACCEPTED");
        verify(channel).basicAck(42L, false);
    }

    @Test
    void rejectedResult_cancelsOrderAndPublishesEvent() throws Exception {
        Order order = pending(2L);
        when(orderRepository.findWithDetailsById(2L)).thenReturn(Optional.of(order));

        listener.handleSagaResult(new OrderSagaResult(2L, false, "Gig deleted"), channel, 43L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishOrderEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RabbitMQConfig.ORDER_CANCELLED_KEY);
        assertThat(captor.getValue().note()).contains("Gig deleted");
        verify(channel).basicAck(43L, false);
    }

    @Test
    void missingOrder_acksWithoutPublishing() throws Exception {
        when(orderRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        listener.handleSagaResult(new OrderSagaResult(99L, true, null), channel, 1L);

        verify(eventPublisher, never()).publishOrderEvent(any());
        verify(channel).basicAck(1L, false);
    }

    @Test
    void nonPendingOrder_isIgnored() throws Exception {
        Order order = pending(1L);
        order.setStatus(OrderStatus.ACCEPTED);
        when(orderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));

        listener.handleSagaResult(new OrderSagaResult(1L, false, "late reject"), channel, 5L);

        verify(eventPublisher, never()).publishOrderEvent(any());
        verify(channel).basicAck(5L, false);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void exception_isNackedAndRequeued() throws Exception {
        when(orderRepository.findWithDetailsById(1L)).thenThrow(new RuntimeException("db down"));

        listener.handleSagaResult(new OrderSagaResult(1L, true, null), channel, 9L);

        verify(channel).basicNack(eq(9L), eq(false), eq(true));
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
