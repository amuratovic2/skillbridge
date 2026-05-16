package com.skillbridge.order.events;

import com.skillbridge.order.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private OrderEventPublisher publisher;

    @Test
    void publishOrderEvent_routesByEventType() {
        OrderEvent ev = OrderEvent.of(RabbitMQConfig.ORDER_ACCEPTED_KEY, 1L, 10, 5, 1,
            "PENDING", "ACCEPTED", new BigDecimal("100"), 5, null);

        publisher.publishOrderEvent(ev);

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.ORDER_EXCHANGE),
            eq(RabbitMQConfig.ORDER_ACCEPTED_KEY),
            any(Object.class)
        );
    }

    @Test
    void publishCustomOfferEvent_routesByEventType() {
        CustomOfferEvent ev = CustomOfferEvent.of(RabbitMQConfig.CUSTOM_OFFER_SENT_KEY,
            42L, 5, 7, 1, "Test", new BigDecimal("100"), "PENDING");

        publisher.publishCustomOfferEvent(ev);

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.ORDER_EXCHANGE),
            eq(RabbitMQConfig.CUSTOM_OFFER_SENT_KEY),
            any(Object.class)
        );
    }

    @Test
    void publishDeliveryEvent_routesByEventType() {
        DeliveryEvent ev = DeliveryEvent.created(1L, 99L, 10, 5, 1, "first draft");

        publisher.publishDeliveryEvent(ev);

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.ORDER_EXCHANGE),
            eq(RabbitMQConfig.DELIVERY_CREATED_KEY),
            any(Object.class)
        );
    }
}
