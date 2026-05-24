package com.skillbridge.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.skillbridge.order.config.RabbitMQConfig;
import com.skillbridge.order.events.CustomOfferEvent;
import com.skillbridge.order.events.OrderEventPublisher;
import com.skillbridge.order.model.CustomOffer;
import com.skillbridge.order.model.CustomOfferStatus;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.repository.CustomOfferRepository;
import com.skillbridge.order.repository.OrderRepository;
import com.skillbridge.order.saga.OrderPlacedEvent;
import com.skillbridge.order.saga.OrderSagaPublisher;

@ExtendWith(MockitoExtension.class)
class CustomOfferServiceTest {

    @Mock private CustomOfferRepository repo;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderEventPublisher eventPublisher;
    @Mock private OrderSagaPublisher sagaPublisher;
    @InjectMocks private CustomOfferService service;

    private CustomOffer pendingOffer(long id, int senderId, int receiverId) {
        CustomOffer o = new CustomOffer();
        o.setId(id);
        o.setSenderId(senderId);
        o.setReceiverId(receiverId);
        o.setTitle("Test ponuda");
        o.setPrice(new BigDecimal("100.00"));
        o.setDeliveryDays(5);
        o.setRevisionCount(2);
        o.setStatus(CustomOfferStatus.PENDING);
        o.setExpiresAt(LocalDateTime.now().plusDays(3));
        return o;
    }

    @Test
    void create_savesPendingOfferAndPublishesSentEvent() {
        CustomOffer input = new CustomOffer();
        input.setReceiverId(7);
        input.setTitle("Logo");
        input.setPrice(new BigDecimal("50"));
        input.setDeliveryDays(3);
        input.setRevisionCount(1);
        when(repo.save(any())).thenAnswer(inv -> {
            CustomOffer o = inv.getArgument(0);
            o.setId(42L);
            return o;
        });

        CustomOffer saved = service.create(5, input);

        assertThat(saved.getSenderId()).isEqualTo(5);
        assertThat(saved.getStatus()).isEqualTo(CustomOfferStatus.PENDING);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

        ArgumentCaptor<CustomOfferEvent> captor = ArgumentCaptor.forClass(CustomOfferEvent.class);
        verify(eventPublisher).publishCustomOfferEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RabbitMQConfig.CUSTOM_OFFER_SENT_KEY);
        assertThat(captor.getValue().offerId()).isEqualTo(42L);
        assertThat(captor.getValue().senderId()).isEqualTo(5);
        assertThat(captor.getValue().receiverId()).isEqualTo(7);
    }

    @Test
    void respond_acceptingOfferPublishesAcceptedEvent() {
        when(repo.findById(1L)).thenReturn(Optional.of(pendingOffer(1L, 5, 7)));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.respond(1L, 7, CustomOfferStatus.ACCEPTED);

        ArgumentCaptor<CustomOfferEvent> captor = ArgumentCaptor.forClass(CustomOfferEvent.class);
        verify(eventPublisher).publishCustomOfferEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RabbitMQConfig.CUSTOM_OFFER_ACCEPTED_KEY);
        assertThat(captor.getValue().status()).isEqualTo("ACCEPTED");
    }

    @Test
    void respond_acceptingGigOfferCreatesOrderFromOfferTerms() {
        CustomOffer offer = pendingOffer(1L, 5, 7);
        offer.setGigId(11);
        offer.setDescription("Custom landing page brief");
        when(repo.findById(1L)).thenReturn(Optional.of(offer));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(77L);
            return order;
        });
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomOffer saved = service.respond(1L, 7, CustomOfferStatus.ACCEPTED);

        assertThat(saved.getOrderId()).isEqualTo(77L);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getClientId()).isEqualTo(7);
        assertThat(orderCaptor.getValue().getSellerId()).isEqualTo(5);
        assertThat(orderCaptor.getValue().getGigId()).isEqualTo(11);
        assertThat(orderCaptor.getValue().getTotalCost()).isEqualByComparingTo("100.00");
        assertThat(orderCaptor.getValue().getRequirements()).isEqualTo("Custom landing page brief");

        ArgumentCaptor<OrderPlacedEvent> sagaCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(sagaPublisher).publishOrderPlaced(sagaCaptor.capture());
        assertThat(sagaCaptor.getValue().orderId()).isEqualTo(77L);
    }

    @Test
    void respond_rejectingOfferPublishesRejectedEvent() {
        when(repo.findById(1L)).thenReturn(Optional.of(pendingOffer(1L, 5, 7)));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.respond(1L, 7, CustomOfferStatus.REJECTED);

        ArgumentCaptor<CustomOfferEvent> captor = ArgumentCaptor.forClass(CustomOfferEvent.class);
        verify(eventPublisher).publishCustomOfferEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RabbitMQConfig.CUSTOM_OFFER_REJECTED_KEY);
    }

    @Test
    void respond_forbidsNonReceiver() {
        when(repo.findById(1L)).thenReturn(Optional.of(pendingOffer(1L, 5, 7)));

        assertThatThrownBy(() -> service.respond(1L, 999, CustomOfferStatus.ACCEPTED))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN));

        verify(eventPublisher, never()).publishCustomOfferEvent(any());
    }

    @Test
    void respond_rejectsAlreadyAcceptedOffer() {
        CustomOffer existing = pendingOffer(1L, 5, 7);
        existing.setStatus(CustomOfferStatus.ACCEPTED);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.respond(1L, 7, CustomOfferStatus.REJECTED))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(eventPublisher, never()).publishCustomOfferEvent(any());
    }

    @Test
    void respond_expiredOfferIsMarkedExpiredAndRejected() {
        CustomOffer existing = pendingOffer(1L, 5, 7);
        existing.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.respond(1L, 7, CustomOfferStatus.ACCEPTED))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));

        assertThat(existing.getStatus()).isEqualTo(CustomOfferStatus.EXPIRED);
    }

    @Test
    void withdraw_publishesWithdrawnEvent() {
        when(repo.findById(1L)).thenReturn(Optional.of(pendingOffer(1L, 5, 7)));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.withdraw(1L, 5);

        ArgumentCaptor<CustomOfferEvent> captor = ArgumentCaptor.forClass(CustomOfferEvent.class);
        verify(eventPublisher).publishCustomOfferEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(RabbitMQConfig.CUSTOM_OFFER_WITHDRAWN_KEY);
        assertThat(captor.getValue().status()).isEqualTo("WITHDRAWN");
    }

    @Test
    void withdraw_forbidsNonSender() {
        when(repo.findById(1L)).thenReturn(Optional.of(pendingOffer(1L, 5, 7)));

        assertThatThrownBy(() -> service.withdraw(1L, 999))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
