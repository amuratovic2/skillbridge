package com.skillbridge.order.repository;

import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByClientIdOrderByOrderDateDesc(Integer clientId, Pageable pageable);
    Page<Order> findBySellerIdOrderByOrderDateDesc(Integer sellerId, Pageable pageable);
    long countByClientId(Integer clientId);

    @EntityGraph(attributePaths = {"history"})
    Optional<Order> findWithDetailsById(Long id);

    @Query("SELECT o FROM Order o WHERE o.clientId = :clientId AND o.status = :status ORDER BY o.orderDate DESC")
    List<Order> findByClientIdAndStatus(
        @Param("clientId") Integer clientId,
        @Param("status") OrderStatus status
    );

    @Query("""
        SELECT o FROM Order o
        WHERE o.deliveryDeadline < :now
          AND o.status NOT IN ('COMPLETED', 'CANCELLED')
        ORDER BY o.deliveryDeadline ASC
    """)
    List<Order> findOverdueOrders(@Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(o.totalCost), 0) FROM Order o WHERE o.clientId = :clientId AND o.status = 'COMPLETED'")
    BigDecimal sumCompletedRevenueByClient(@Param("clientId") Integer clientId);

    @Query(value = """
        SELECT status, COUNT(*) as count
        FROM orders.orders
        GROUP BY status
    """, nativeQuery = true)
    List<Object[]> countByStatusNative();
}