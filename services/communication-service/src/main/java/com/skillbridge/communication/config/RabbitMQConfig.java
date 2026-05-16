package com.skillbridge.communication.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Subscribes communication-service to the topic exchange driven by order-service.
 * One queue per concern is bound with a wildcard so we get every relevant event
 * without the publisher needing to know who is listening.
 */
@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "skillbridge.orders";
    public static final String MESSAGE_EXCHANGE = "skillbridge.messages";

    public static final String ORDER_NOTIFICATIONS_QUEUE = "communication.order-notifications";
    public static final String OFFER_NOTIFICATIONS_QUEUE = "communication.offer-notifications";
    public static final String DELIVERY_NOTIFICATIONS_QUEUE = "communication.delivery-notifications";
    public static final String MESSAGE_RECEIVED_QUEUE = "communication.messages-received";

    public static final String MESSAGE_SENT_KEY = "message.sent";

    @Bean
    TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(ORDER_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue orderNotificationsQueue() {
        return QueueBuilder.durable(ORDER_NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    Queue offerNotificationsQueue() {
        return QueueBuilder.durable(OFFER_NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    Queue deliveryNotificationsQueue() {
        return QueueBuilder.durable(DELIVERY_NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    Binding orderNotificationsBinding(Queue orderNotificationsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderNotificationsQueue).to(orderExchange).with("order.*");
    }

    @Bean
    Binding offerNotificationsBinding(Queue offerNotificationsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(offerNotificationsQueue).to(orderExchange).with("custom-offer.*");
    }

    @Bean
    Binding deliveryNotificationsBinding(Queue deliveryNotificationsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(deliveryNotificationsQueue).to(orderExchange).with("delivery.*");
    }

    @Bean
    TopicExchange messageExchange() {
        return ExchangeBuilder.topicExchange(MESSAGE_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue messageReceivedQueue() {
        return QueueBuilder.durable(MESSAGE_RECEIVED_QUEUE).build();
    }

    @Bean
    Binding messageReceivedBinding(Queue messageReceivedQueue, TopicExchange messageExchange) {
        return BindingBuilder.bind(messageReceivedQueue).to(messageExchange).with("message.*");
    }

    @Bean
    Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        Jackson2JsonMessageConverter converter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}
