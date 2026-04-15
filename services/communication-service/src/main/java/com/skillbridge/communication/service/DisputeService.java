package com.skillbridge.communication.service;

import com.skillbridge.communication.model.Dispute;
import com.skillbridge.communication.model.DisputeStatus;
import com.skillbridge.communication.repository.DisputeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;

    public DisputeService(DisputeRepository disputeRepository) {
        this.disputeRepository = disputeRepository;
    }

    public Dispute create(Integer initiatorId, Integer orderId, String reason, String description) {
        boolean activeExists = disputeRepository.existsByOrderIdAndStatusIn(
            orderId, List.of(DisputeStatus.OPEN, DisputeStatus.UNDER_REVIEW)
        );
        if (activeExists) {
            throw new IllegalStateException("An active dispute already exists for this order");
        }

        Dispute dispute = new Dispute();
        dispute.setOrderId(orderId);
        dispute.setInitiatorId(initiatorId);
        dispute.setReason(reason);
        dispute.setDescription(description);
        dispute.setCreatedAt(LocalDateTime.now());
        return disputeRepository.save(dispute);
    }

    public Dispute findById(Integer id) {
        return disputeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
    }

    public Map<String, Object> findAll(DisputeStatus status, int page, int limit) {
        Page<Dispute> result;
        if (status != null) {
            result = disputeRepository.findByStatus(status, PageRequest.of(page - 1, limit));
        } else {
            result = disputeRepository.findAll(PageRequest.of(page - 1, limit));
        }

        return Map.of(
            "data", result.getContent(),
            "meta", Map.of(
                "total", result.getTotalElements(),
                "page", page,
                "limit", limit,
                "totalPages", result.getTotalPages()
            )
        );
    }

    public Dispute assign(Integer id, Integer adminId) {
        Dispute dispute = findById(id);
        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new IllegalStateException("Only OPEN disputes can be assigned");
        }
        dispute.setAdminId(adminId);
        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        return disputeRepository.save(dispute);
    }

    public Dispute resolve(Integer id, Integer adminId, String resolution, DisputeStatus status) {
        Dispute dispute = findById(id);
        if (!adminId.equals(dispute.getAdminId())) {
            throw new IllegalStateException("Only the assigned admin can resolve this dispute");
        }
        if (dispute.getStatus() != DisputeStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Only UNDER_REVIEW disputes can be resolved");
        }
        dispute.setResolution(resolution);
        dispute.setStatus(status);
        dispute.setResolvedAt(LocalDateTime.now());
        return disputeRepository.save(dispute);
    }
}
