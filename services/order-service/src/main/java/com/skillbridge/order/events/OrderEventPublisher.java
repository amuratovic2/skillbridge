package com.skillbridge.order.events;

import com.skillbridge.order.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Fans order, custom-offer and delivery lifecycle events out onto the
 * skillbridge.orders topic exchange so any interested service (notifications,
 * analytics, ...) can consume them asynchronously.
 *
 * Routing keys are kept stable strings so subscribers can bind with wildcards
 * (e.g. {@code order.*} or {@code custom-offer.*}).
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderEvent(OrderEvent event) {
        String routingKey = event.eventType();
        log.debug("Publishing {} for orderId={}", routingKey, event.orderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, routingKey, event);
    }

    public void publishCustomOfferEvent(CustomOfferEvent event) {
        String routingKey = event.eventType();
        log.debug("Publishing {} for offerId={}", routingKey, event.offerId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, routingKey, event);
    }

    public void publishDeliveryEvent(DeliveryEvent event) {
        String routingKey = event.eventType();
        log.debug("Publishing {} for orderId={}", routingKey, event.orderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, routingKey, event);
    }
}
