package com.skillbridge.order.repository;

import com.skillbridge.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByClientIdOrderByOrderDateDesc(Integer clientId, Pageable pageable);

    long countByClientId(Integer clientId);
}
