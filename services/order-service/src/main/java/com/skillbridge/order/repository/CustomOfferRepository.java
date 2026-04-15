package com.skillbridge.order.repository;

import com.skillbridge.order.model.CustomOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomOfferRepository extends JpaRepository<CustomOffer, Long> {

    List<CustomOffer> findByReceiverIdOrderByCreatedAtDesc(Integer receiverId);

    List<CustomOffer> findBySenderIdOrderByCreatedAtDesc(Integer senderId);
}
