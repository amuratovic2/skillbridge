package com.skillbridge.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.communication.dto.RemoteUserProfile;
import com.skillbridge.communication.dto.SendMessageRequest;
import com.skillbridge.communication.messaging.MessageEventPublisher;
import com.skillbridge.communication.model.Message;
import com.skillbridge.communication.repository.MessageRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceUserValidationTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserDirectoryClient userDirectoryClient;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(
            messageRepository,
            new PageableFactory(),
            new ObjectMapper(),
            Validation.buildDefaultValidatorFactory().getValidator(),
            userDirectoryClient,
            messageEventPublisher,
            true
        );
    }

    @Test
    void sendValidatesSenderAndReceiverBeforeSaving() {
        when(userDirectoryClient.findActiveUser(1))
            .thenReturn(Optional.of(new RemoteUserProfile(1, "sender", "Sender", "User", true)));
        when(userDirectoryClient.findActiveUser(2))
            .thenReturn(Optional.of(new RemoteUserProfile(2, "receiver", "Receiver", "User", true)));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        messageService.send(1, new SendMessageRequest(2, null, "Zdravo"));

        verify(userDirectoryClient).findActiveUser(1);
        verify(userDirectoryClient).findActiveUser(2);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendRejectsMissingReceiverBeforeSaving() {
        when(userDirectoryClient.findActiveUser(1))
            .thenReturn(Optional.of(new RemoteUserProfile(1, "sender", "Sender", "User", true)));
        when(userDirectoryClient.findActiveUser(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.send(1, new SendMessageRequest(2, null, "Zdravo")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendPropagatesUnavailableUserServiceAsServiceUnavailable() {
        when(userDirectoryClient.findActiveUser(1))
            .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User service is unavailable"));

        assertThatThrownBy(() -> messageService.send(1, new SendMessageRequest(2, null, "Zdravo")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(messageRepository, never()).save(any(Message.class));
    }
}
