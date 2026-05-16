package com.skillbridge.order.service;

import com.skillbridge.order.config.RabbitMQConfig;
import com.skillbridge.order.events.CustomOfferEvent;
import com.skillbridge.order.events.OrderEventPublisher;
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
    private final OrderEventPublisher eventPublisher;

    public CustomOfferService(CustomOfferRepository customOfferRepository,
                              OrderEventPublisher eventPublisher) {
        this.customOfferRepository = customOfferRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CustomOffer create(Integer senderId, CustomOffer offer) {
        offer.setSenderId(senderId);
        offer.setStatus(CustomOfferStatus.PENDING);
        offer.setExpiresAt(LocalDateTime.now().plusDays(7));
        CustomOffer saved = customOfferRepository.save(offer);

        eventPublisher.publishCustomOfferEvent(CustomOfferEvent.of(
            RabbitMQConfig.CUSTOM_OFFER_SENT_KEY, saved.getId(), saved.getSenderId(),
            saved.getReceiverId(), saved.getGigId(), saved.getTitle(),
            saved.getPrice(), saved.getStatus().name()
        ));

        return saved;
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
        CustomOffer saved = customOfferRepository.save(offer);

        String routingKey = switch (status) {
            case ACCEPTED -> RabbitMQConfig.CUSTOM_OFFER_ACCEPTED_KEY;
            case REJECTED -> RabbitMQConfig.CUSTOM_OFFER_REJECTED_KEY;
            default -> null;
        };
        if (routingKey != null) {
            eventPublisher.publishCustomOfferEvent(CustomOfferEvent.of(
                routingKey, saved.getId(), saved.getSenderId(), saved.getReceiverId(),
                saved.getGigId(), saved.getTitle(), saved.getPrice(), saved.getStatus().name()
            ));
        }
        return saved;
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
        CustomOffer saved = customOfferRepository.save(offer);

        eventPublisher.publishCustomOfferEvent(CustomOfferEvent.of(
            RabbitMQConfig.CUSTOM_OFFER_WITHDRAWN_KEY, saved.getId(), saved.getSenderId(),
            saved.getReceiverId(), saved.getGigId(), saved.getTitle(),
            saved.getPrice(), saved.getStatus().name()
        ));

        return saved;
    }
}
