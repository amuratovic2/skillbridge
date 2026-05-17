package com.skillbridge.gig.saga;

import com.rabbitmq.client.Channel;
import com.skillbridge.gig.model.Gig;
import com.skillbridge.gig.repository.GigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private GigRepository gigRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private Channel channel;

    @Test
    void handleOrderTerminalEvent_decrementsActiveOrderCount() throws Exception {
        Gig gig = new Gig();
        gig.setId(1);
        gig.setActiveOrderCount(2);
        when(gigRepository.findById(1)).thenReturn(Optional.of(gig));
        OrderEventListener listener = new OrderEventListener(gigRepository, rabbitTemplate);
        ArgumentCaptor<Gig> gigCaptor = ArgumentCaptor.forClass(Gig.class);

        listener.handleOrderTerminalEvent(new OrderTerminalEvent(10L, 1), channel, 99L);

        verify(gigRepository).save(gigCaptor.capture());
        assertThat(gigCaptor.getValue().getActiveOrderCount()).isEqualTo(1);
        verify(channel).basicAck(99L, false);
    }

    @Test
    void handleOrderTerminalEvent_doesNotGoBelowZero() throws Exception {
        Gig gig = new Gig();
        gig.setId(1);
        gig.setActiveOrderCount(0);
        when(gigRepository.findById(1)).thenReturn(Optional.of(gig));
        OrderEventListener listener = new OrderEventListener(gigRepository, rabbitTemplate);
        ArgumentCaptor<Gig> gigCaptor = ArgumentCaptor.forClass(Gig.class);

        listener.handleOrderTerminalEvent(new OrderTerminalEvent(10L, 1), channel, 99L);

        verify(gigRepository).save(gigCaptor.capture());
        assertThat(gigCaptor.getValue().getActiveOrderCount()).isZero();
        verify(channel).basicAck(99L, false);
    }

    @Test
    void handleOrderTerminalEvent_acknowledgesMissingGigWithoutSave() throws Exception {
        when(gigRepository.findById(1)).thenReturn(Optional.empty());
        OrderEventListener listener = new OrderEventListener(gigRepository, rabbitTemplate);

        listener.handleOrderTerminalEvent(new OrderTerminalEvent(10L, 1), channel, 99L);

        verify(gigRepository, never()).save(any());
        verify(channel).basicAck(99L, false);
    }
}
