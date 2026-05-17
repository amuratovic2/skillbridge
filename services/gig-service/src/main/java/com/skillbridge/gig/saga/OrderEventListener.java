package com.skillbridge.gig.saga;

import com.rabbitmq.client.Channel;
import com.skillbridge.gig.config.RabbitMQConfig;
import com.skillbridge.gig.model.Gig;
import com.skillbridge.gig.model.GigStatus;
import com.skillbridge.gig.repository.GigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Saga participant in gig-service.
 *
 * Receives order.placed → validates gig → performs local transaction (increment activeOrderCount)
 * → publishes order.confirmed or order.rejected back to order-service.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final GigRepository gigRepository;
    private final RabbitTemplate rabbitTemplate;

    public OrderEventListener(GigRepository gigRepository, RabbitTemplate rabbitTemplate) {
        this.gigRepository = gigRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.GIG_ORDER_EVENTS_QUEUE)
    @Transactional
    public void handleOrderPlaced(
        OrderPlacedEvent event,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.info("Saga: received order.placed – orderId={} gigId={}", event.orderId(), event.gigId());
        try {
            Gig gig = gigRepository.findById(event.gigId()).orElse(null);

            if (gig == null || gig.getStatus() == GigStatus.DELETED) {
                publishResult(event.orderId(), false, "Gig not found");
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (gig.getStatus() != GigStatus.ACTIVE) {
                // Gig is paused or deleted – reject the order
                publishResult(event.orderId(), false,
                    "Gig nije aktivan (status: " + gig.getStatus() + ")");
                log.warn("Saga: gig {} is not ACTIVE – rejecting orderId={}", event.gigId(), event.orderId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // Local transaction: increment active order count on the gig
            gig.setActiveOrderCount(gig.getActiveOrderCount() + 1);
            gigRepository.save(gig);

            publishResult(event.orderId(), true, null);
            log.info("Saga: gig {} confirmed – orderId={}, activeOrders={}",
                event.gigId(), event.orderId(), gig.getActiveOrderCount());

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Saga: error handling order.placed for orderId={}: {}", event.orderId(), e.getMessage());
            publishResult(event.orderId(), false, "Interna greška u gig-service");
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.GIG_ORDER_TERMINAL_EVENTS_QUEUE)
    @Transactional
    public void handleOrderTerminalEvent(
        OrderTerminalEvent event,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.info("Saga: received terminal order event - orderId={} gigId={}", event.orderId(), event.gigId());
        try {
            Gig gig = gigRepository.findById(event.gigId()).orElse(null);
            if (gig == null) {
                log.warn("Saga: terminal event for missing gig {} - orderId={}", event.gigId(), event.orderId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            gig.setActiveOrderCount(Math.max(0, gig.getActiveOrderCount() - 1));
            gigRepository.save(gig);

            log.info("Saga: decremented active order count for gig {} - orderId={}, activeOrders={}",
                event.gigId(), event.orderId(), gig.getActiveOrderCount());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Saga: error handling terminal order event for orderId={}: {}", event.orderId(), e.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private void publishResult(Long orderId, boolean confirmed, String reason) {
        String routingKey = confirmed ? RabbitMQConfig.ORDER_CONFIRMED_KEY : RabbitMQConfig.ORDER_REJECTED_KEY;
        OrderSagaResult result = new OrderSagaResult(orderId, confirmed, reason);
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, routingKey, result);
        log.info("Saga: published {} for orderId={}", routingKey, orderId);
    }
}
