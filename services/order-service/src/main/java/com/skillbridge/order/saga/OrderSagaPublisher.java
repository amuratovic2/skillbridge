package com.skillbridge.order.saga;

import com.skillbridge.order.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderSagaPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderPlaced(OrderPlacedEvent event) {
        log.info("Saga: publishing order.placed for orderId={} gigId={}", event.orderId(), event.gigId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_PLACED_KEY, event);
    }
}
