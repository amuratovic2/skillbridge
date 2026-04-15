package com.skillbridge.order.config;

import com.skillbridge.order.model.*;
import com.skillbridge.order.repository.DeliveryRepository;
import com.skillbridge.order.repository.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;

    public DataSeeder(OrderRepository orderRepository, DeliveryRepository deliveryRepository) {
        this.orderRepository = orderRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (orderRepository.count() > 0) {
            System.out.println("Order data already seeded, skipping.");
            return;
        }

        System.out.println("Seeding order data...");

        // Order 1: clientId=7, gigId=1, cost=150, COMPLETED, maxRevisions=5, usedRevisions=1
        Order order1 = createOrder(7, 1, new BigDecimal("150"), OrderStatus.COMPLETED, 5, 1, 7);
        order1.setCompletedAt(LocalDateTime.now().minusDays(2));
        order1.setOrderDate(LocalDateTime.now().minusDays(15));
        order1.setDeliveryDeadline(LocalDateTime.now().minusDays(5));
        orderRepository.save(order1);
        createDelivery(order1, 1, "Final delivery for logo design", null, null);

        // Order 2: clientId=8, gigId=3, cost=500, IN_PROGRESS, maxRevisions=3
        Order order2 = createOrder(8, 3, new BigDecimal("500"), OrderStatus.IN_PROGRESS, 3, 0, 14);
        order2.setOrderDate(LocalDateTime.now().minusDays(5));
        orderRepository.save(order2);

        // Order 3: clientId=7, gigId=5, cost=80, DELIVERED, maxRevisions=2
        Order order3 = createOrder(7, 5, new BigDecimal("80"), OrderStatus.DELIVERED, 2, 0, 5);
        order3.setOrderDate(LocalDateTime.now().minusDays(8));
        orderRepository.save(order3);
        createDelivery(order3, 1, "Video editing completed", null, null);

        // Order 4: clientId=8, gigId=7, cost=200, COMPLETED, maxRevisions=2
        Order order4 = createOrder(8, 7, new BigDecimal("200"), OrderStatus.COMPLETED, 2, 0, 7);
        order4.setCompletedAt(LocalDateTime.now().minusDays(1));
        order4.setOrderDate(LocalDateTime.now().minusDays(10));
        order4.setDeliveryDeadline(LocalDateTime.now().minusDays(3));
        orderRepository.save(order4);
        createDelivery(order4, 1, "SEO report delivered", null, null);

        // Order 5: clientId=7, gigId=9, cost=350, PENDING, maxRevisions=4
        Order order5 = createOrder(7, 9, new BigDecimal("350"), OrderStatus.PENDING, 4, 0, 10);
        order5.setOrderDate(LocalDateTime.now().minusDays(1));
        orderRepository.save(order5);

        System.out.println("=== Order Seed Summary ===");
        System.out.println("  Orders: 5");
        System.out.println("  Deliveries: 3");
        System.out.println("==========================");
    }

    private Order createOrder(int clientId, int gigId, BigDecimal cost, OrderStatus status,
                              int maxRevisions, int usedRevisions, int deliveryDays) {
        Order order = new Order();
        order.setClientId(clientId);
        order.setGigId(gigId);
        order.setTotalCost(cost);
        order.setStatus(status);
        order.setMaxRevisions(maxRevisions);
        order.setUsedRevisions(usedRevisions);
        order.setDeliveryDeadline(LocalDateTime.now().plusDays(deliveryDays));

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setChangedByUserId((long) clientId);
        history.setActionType("ORDER_CREATED");
        history.setNewStatus(OrderStatus.PENDING.name());
        order.getHistory().add(history);

        return orderRepository.save(order);
    }

    private void createDelivery(Order order, int version, String message, String fileUrl, String fileName) {
        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setVersionNumber(version);
        delivery.setMessage(message);
        delivery.setFileUrl(fileUrl);
        delivery.setFileName(fileName);
        deliveryRepository.save(delivery);
    }
}
