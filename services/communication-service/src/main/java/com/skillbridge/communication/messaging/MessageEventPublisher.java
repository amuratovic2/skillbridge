package com.skillbridge.communication.messaging;

import com.skillbridge.communication.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public MessageEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishMessageSent(MessageSentEvent event) {
        log.debug("Publishing message.sent for messageId={}", event.messageId());
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MESSAGE_EXCHANGE,
            RabbitMQConfig.MESSAGE_SENT_KEY,
            event
        );
    }
}
