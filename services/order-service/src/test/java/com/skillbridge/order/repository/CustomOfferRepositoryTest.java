package com.skillbridge.order.repository;

import com.skillbridge.order.model.CustomOffer;
import com.skillbridge.order.model.OfferStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CustomOfferRepositoryTest {

    @Autowired
    private CustomOfferRepository repository;

    @Test
    void saveCustomOffer_success() {
        CustomOffer offer = new CustomOffer();
        offer.setSenderId(1);
        offer.setReceiverId(2);
        offer.setTitle("Test Offer");
        offer.setPrice(new BigDecimal("50"));
        offer.setStatus(OfferStatus.PENDING);

        CustomOffer saved = repository.save(offer);

        assertNotNull(saved.getId());
        assertEquals("Test Offer", saved.getTitle());
    }
}