package com.skillbridge.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange that all order-saga messages flow through
    public static final String ORDER_EXCHANGE = "skillbridge.orders";

    // Saga routing keys
    public static final String ORDER_PLACED_KEY   = "order.placed";
    public static final String ORDER_CONFIRMED_KEY = "order.confirmed";
    public static final String ORDER_REJECTED_KEY  = "order.rejected";

    // Lifecycle routing keys – published by order-service, consumed by
    // communication-service (notifications), gig-service (terminal events),
    // and any future subscribers.
    public static final String ORDER_ACCEPTED_KEY  = "order.accepted";
    public static final String ORDER_VALIDATED_KEY = "order.validated";
    public static final String ORDER_IN_PROGRESS_KEY = "order.in-progress";
    public static final String ORDER_DELIVERED_KEY = "order.delivered";
    public static final String ORDER_COMPLETED_KEY = "order.completed";
    public static final String ORDER_CANCELLED_KEY = "order.cancelled";
    public static final String ORDER_REVISION_REQUESTED_KEY = "order.revision-requested";

    public static final String CUSTOM_OFFER_SENT_KEY     = "custom-offer.sent";
    public static final String CUSTOM_OFFER_ACCEPTED_KEY = "custom-offer.accepted";
    public static final String CUSTOM_OFFER_REJECTED_KEY = "custom-offer.rejected";
    public static final String CUSTOM_OFFER_WITHDRAWN_KEY = "custom-offer.withdrawn";

    public static final String DELIVERY_CREATED_KEY = "delivery.created";

    // Queues
    public static final String GIG_ORDER_EVENTS_QUEUE  = "gig.order-events";      // gig-service listens
    public static final String GIG_ORDER_TERMINAL_EVENTS_QUEUE = "gig.order-terminal-events";
    public static final String ORDER_SAGA_RESULTS_QUEUE = "order.saga-results";   // order-service listens

    @Bean
    TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(ORDER_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue gigOrderEventsQueue() {
        return QueueBuilder.durable(GIG_ORDER_EVENTS_QUEUE).build();
    }

    @Bean
    Queue gigOrderTerminalEventsQueue() {
        return QueueBuilder.durable(GIG_ORDER_TERMINAL_EVENTS_QUEUE).build();
    }

    @Bean
    Queue orderSagaResultsQueue() {
        return QueueBuilder.durable(ORDER_SAGA_RESULTS_QUEUE).build();
    }

    @Bean
    Binding gigOrderEventsBinding(Queue gigOrderEventsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(gigOrderEventsQueue).to(orderExchange).with(ORDER_PLACED_KEY);
    }

    @Bean
    Binding gigOrderCancelledBinding(Queue gigOrderTerminalEventsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(gigOrderTerminalEventsQueue).to(orderExchange).with(ORDER_CANCELLED_KEY);
    }

    @Bean
    Binding gigOrderCompletedBinding(Queue gigOrderTerminalEventsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(gigOrderTerminalEventsQueue).to(orderExchange).with(ORDER_COMPLETED_KEY);
    }

    @Bean
    Binding orderConfirmedBinding(Queue orderSagaResultsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderSagaResultsQueue).to(orderExchange).with(ORDER_CONFIRMED_KEY);
    }

    @Bean
    Binding orderRejectedBinding(Queue orderSagaResultsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderSagaResultsQueue).to(orderExchange).with(ORDER_REJECTED_KEY);
    }

    @Bean
    Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  Jackson2JsonMessageConverter converter) {
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
