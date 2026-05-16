package com.skillbridge.order.saga;

import com.rabbitmq.client.Channel;
import com.skillbridge.order.config.RabbitMQConfig;
import com.skillbridge.order.events.OrderEvent;
import com.skillbridge.order.events.OrderEventPublisher;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderHistory;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Listens to saga result events from gig-service.
 *
 * order.confirmed → set order ACCEPTED   (both local transactions succeeded – saga is final)
 * order.rejected  → set order CANCELLED  (compensating transaction – gig was unavailable)
 */
@Component
public class OrderSagaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaListener.class);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderSagaListener(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_SAGA_RESULTS_QUEUE)
    @Transactional
    public void handleSagaResult(
        OrderSagaResult result,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {
            Order order = orderRepository.findWithDetailsById(result.orderId()).orElse(null);
            if (order == null) {
                log.warn("Saga result received for unknown orderId={}", result.orderId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (order.getStatus() != OrderStatus.PENDING) {
                log.warn("Saga result received but order {} is no longer PENDING (status={}), ignoring",
                    result.orderId(), order.getStatus());
                channel.basicAck(deliveryTag, false);
                return;
            }

            String routingKey;
            String note;
            if (result.confirmed()) {
                // Gig-service confirmed the order – both local transactions succeeded
                order.setStatus(OrderStatus.ACCEPTED);
                note = "Gig potvrđen – narudžba aktivna";
                addHistory(order, "SAGA_CONFIRMED", OrderStatus.PENDING, OrderStatus.ACCEPTED, note);
                routingKey = RabbitMQConfig.ORDER_ACCEPTED_KEY;
                log.info("Saga COMPLETED: orderId={} → ACCEPTED", result.orderId());
            } else {
                // Compensating transaction: gig was unavailable or inactive
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(LocalDateTime.now());
                note = "Gig nedostupan – narudžba otkazana: " + result.reason();
                addHistory(order, "SAGA_COMPENSATED", OrderStatus.PENDING, OrderStatus.CANCELLED, note);
                routingKey = RabbitMQConfig.ORDER_CANCELLED_KEY;
                log.warn("Saga COMPENSATED: orderId={} → CANCELLED, reason={}", result.orderId(), result.reason());
            }

            orderRepository.save(order);

            eventPublisher.publishOrderEvent(OrderEvent.of(
                routingKey, order.getId(), order.getClientId(), order.getSellerId(),
                order.getGigId(), OrderStatus.PENDING.name(), order.getStatus().name(),
                order.getTotalCost(), null, note
            ));

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Error processing saga result for orderId={}: {}", result.orderId(), e.getMessage());
            // Requeue so it can be retried
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private void addHistory(Order order, String action, OrderStatus from, OrderStatus to, String note) {
        OrderHistory h = new OrderHistory();
        h.setOrder(order);
        h.setChangedByUserId(0L);
        h.setActionType(action);
        h.setOldStatus(from.name());
        h.setNewStatus(to.name());
        h.setNote(note);
        order.getHistory().add(h);
    }
}
