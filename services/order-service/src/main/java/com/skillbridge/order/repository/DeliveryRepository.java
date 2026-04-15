package com.skillbridge.order.repository;

import com.skillbridge.order.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByOrderIdOrderByVersionNumberDesc(Long orderId);

    Optional<Delivery> findByOrderIdAndVersionNumber(Long orderId, int versionNumber);
}
