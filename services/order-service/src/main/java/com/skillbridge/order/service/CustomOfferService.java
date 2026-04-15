package com.skillbridge.order.service;

import com.skillbridge.order.model.CustomOffer;
import com.skillbridge.order.model.CustomOfferStatus;
import com.skillbridge.order.repository.CustomOfferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomOfferService {

    private final CustomOfferRepository customOfferRepository;

    public CustomOfferService(CustomOfferRepository customOfferRepository) {
        this.customOfferRepository = customOfferRepository;
    }

    @Transactional
    public CustomOffer create(Integer senderId, CustomOffer offer) {
        offer.setSenderId(senderId);
        offer.setStatus(CustomOfferStatus.PENDING);
        offer.setExpiresAt(LocalDateTime.now().plusDays(7));
        return customOfferRepository.save(offer);
    }

    public List<CustomOffer> findReceived(Integer userId) {
        return customOfferRepository.findByReceiverIdOrderByCreatedAtDesc(userId);
    }

    public List<CustomOffer> findSent(Integer userId) {
        return customOfferRepository.findBySenderIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public CustomOffer respond(Long offerId, Integer userId, CustomOfferStatus status) {
        CustomOffer offer = customOfferRepository.findById(offerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom offer not found"));

        if (!offer.getReceiverId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the receiver can respond to this offer");
        }
        if (offer.getStatus() != CustomOfferStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offer is no longer pending");
        }
        if (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(LocalDateTime.now())) {
            offer.setStatus(CustomOfferStatus.EXPIRED);
            customOfferRepository.save(offer);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offer has expired");
        }

        offer.setStatus(status);
        return customOfferRepository.save(offer);
    }

    @Transactional
    public CustomOffer withdraw(Long offerId, Integer senderId) {
        CustomOffer offer = customOfferRepository.findById(offerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom offer not found"));

        if (!offer.getSenderId().equals(senderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the sender can withdraw this offer");
        }
        if (offer.getStatus() != CustomOfferStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only withdraw a pending offer");
        }

        offer.setStatus(CustomOfferStatus.WITHDRAWN);
        return customOfferRepository.save(offer);
    }
}
