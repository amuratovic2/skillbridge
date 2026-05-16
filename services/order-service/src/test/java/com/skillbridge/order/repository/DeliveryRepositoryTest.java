package com.skillbridge.order.repository;

import com.skillbridge.order.model.Delivery;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DeliveryRepositoryTest {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void saveDelivery_success() {
        Order order = new Order();
        order.setClientId(1);
        order.setGigId(1);
        order.setTotalCost(new BigDecimal("100"));
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        Delivery delivery = new Delivery();
        delivery.setOrder(savedOrder);
        delivery.setMessage("Test delivery");
        delivery.setVersionNumber(1);

        Delivery saved = deliveryRepository.save(delivery);

        assertNotNull(saved.getId());
        assertEquals("Test delivery", saved.getMessage());
    }
}