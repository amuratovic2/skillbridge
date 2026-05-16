package com.skillbridge.order.repository;

import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository repository;

    private Order newOrder(Integer clientId, Integer sellerId, OrderStatus status, BigDecimal cost) {
        Order o = new Order();
        o.setClientId(clientId);
        o.setSellerId(sellerId);
        o.setGigId(99);
        o.setTotalCost(cost);
        o.setStatus(status);
        return o;
    }

    @Test
    void saveAndFindById_success() {
        Order saved = repository.save(newOrder(1, 2, OrderStatus.PENDING, new BigDecimal("100")));

        assertNotNull(saved.getId());
        Order found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(OrderStatus.PENDING, found.getStatus());
        assertEquals(0, new BigDecimal("100").compareTo(found.getTotalCost()));
    }

    @Test
    void findByClientId_returnsOnlyMatchingOrders() {
        repository.save(newOrder(1, 2, OrderStatus.PENDING, new BigDecimal("50")));
        repository.save(newOrder(1, 3, OrderStatus.COMPLETED, new BigDecimal("75")));
        repository.save(newOrder(9, 2, OrderStatus.PENDING, new BigDecimal("60")));

        Page<Order> page = repository.findByClientIdOrderByOrderDateDesc(1, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(o -> o.getClientId() == 1));
    }

    @Test
    void findByClientIdAndStatus_filtersByStatus() {
        repository.save(newOrder(1, 2, OrderStatus.PENDING, new BigDecimal("10")));
        repository.save(newOrder(1, 2, OrderStatus.COMPLETED, new BigDecimal("20")));
        repository.save(newOrder(1, 2, OrderStatus.COMPLETED, new BigDecimal("30")));

        List<Order> completed = repository.findByClientIdAndStatus(1, OrderStatus.COMPLETED);

        assertEquals(2, completed.size());
        assertTrue(completed.stream().allMatch(o -> o.getStatus() == OrderStatus.COMPLETED));
    }

    @Test
    void sumCompletedRevenueBySeller_sumsOnlyCompletedForSeller() {
        repository.save(newOrder(1, 2, OrderStatus.COMPLETED, new BigDecimal("100")));
        repository.save(newOrder(1, 2, OrderStatus.COMPLETED, new BigDecimal("250")));
        repository.save(newOrder(1, 2, OrderStatus.PENDING, new BigDecimal("999")));
        repository.save(newOrder(9, 3, OrderStatus.COMPLETED, new BigDecimal("500")));

        BigDecimal total = repository.sumCompletedRevenueBySeller(2);

        assertEquals(0, new BigDecimal("350").compareTo(total));
    }

    @Test
    void countByClientId_returnsCorrectCount() {
        repository.save(newOrder(1, 2, OrderStatus.PENDING, new BigDecimal("10")));
        repository.save(newOrder(1, 2, OrderStatus.COMPLETED, new BigDecimal("20")));
        repository.save(newOrder(2, 1, OrderStatus.PENDING, new BigDecimal("30")));

        assertEquals(2, repository.countByClientId(1));
        assertEquals(1, repository.countByClientId(2));
    }
}
