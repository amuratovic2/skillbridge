package com.skillbridge.order.repository;

import com.skillbridge.order.model.Delivery;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByOrderIdOrderByVersionNumberDesc(Long orderId);

    Optional<Delivery> findByOrderIdAndVersionNumber(Long orderId, int versionNumber);

    @EntityGraph(attributePaths = "order")
    @Query("SELECT d FROM Delivery d WHERE d.order.id = :orderId ORDER BY d.versionNumber DESC")
    List<Delivery> findDeliveriesWithOrder(@Param("orderId") Long orderId);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.order.id = :orderId")
    long countByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT d FROM Delivery d WHERE d.order.id = :orderId ORDER BY d.versionNumber DESC LIMIT 1")
    Optional<Delivery> findLatestByOrderId(@Param("orderId") Long orderId);
}