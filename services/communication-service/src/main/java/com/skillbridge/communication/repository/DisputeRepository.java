package com.skillbridge.communication.repository;

import com.skillbridge.communication.model.Dispute;
import com.skillbridge.communication.model.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Integer> {

    Page<Dispute> findByStatus(DisputeStatus status, Pageable pageable);

    boolean existsByOrderIdAndStatusIn(Integer orderId, List<DisputeStatus> statuses);
}
