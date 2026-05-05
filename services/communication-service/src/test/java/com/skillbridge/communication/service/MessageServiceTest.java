package com.skillbridge.communication.service;

import com.skillbridge.communication.dto.SendMessageRequest;
import com.skillbridge.communication.dto.BatchSendMessagesRequest;
import com.skillbridge.communication.model.Message;
import com.skillbridge.communication.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class MessageServiceTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
    }

    @Test
    void sendTrimsContentAndStoresUnreadMessage() {
        var response = messageService.send(
            1,
            new SendMessageRequest(2, 15, "  Cao, poslao sam novi update.  ")
        );

        assertThat(response.id()).isNotNull();
        assertThat(response.senderId()).isEqualTo(1);
        assertThat(response.receiverId()).isEqualTo(2);
        assertThat(response.orderId()).isEqualTo(15);
        assertThat(response.content()).isEqualTo("Cao, poslao sam novi update.");
        assertThat(response.isRead()).isFalse();
    }

    @Test
    void sendRejectsMessageToSelf() {
        assertThatThrownBy(() -> messageService.send(1, new SendMessageRequest(1, null, "Poruka sebi")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void sendBatchSavesAllMessagesWhenEveryMessageIsValid() {
        var response = messageService.sendBatch(
            1,
            new BatchSendMessagesRequest(List.of(
                new SendMessageRequest(2, 15, "Prva"),
                new SendMessageRequest(3, 15, "Druga")
            ))
        );

        assertThat(response).hasSize(2);
        assertThat(messageRepository.count()).isEqualTo(2);
        assertThat(response).extracting("receiverId").containsExactly(2, 3);
    }

    @Test
    void sendBatchIsTransactionalWhenOneMessageIsInvalid() {
        assertThatThrownBy(() -> messageService.sendBatch(
            1,
            new BatchSendMessagesRequest(List.of(
                new SendMessageRequest(2, 15, "Validna"),
                new SendMessageRequest(1, 15, "Nevalidna")
            ))
        )).isInstanceOf(ResponseStatusException.class);

        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void markAsReadOnlyUpdatesMessagesForCurrentReceiver() {
        createMessage(2, 1, "Prva", false);
        createMessage(2, 1, "Druga", false);
        createMessage(1, 2, "Moja poruka", false);

        int updated = messageService.markAsRead(1, 2);

        assertThat(updated).isEqualTo(2);
        assertThat(messageRepository.findAll().stream().filter(Message::getIsRead)).hasSize(2);
    }

    @Test
    void getConversationReturnsOnlySelectedParticipantsWithMeta() {
        createMessage(1, 2, 20, "Prva", LocalDateTime.now().minusMinutes(3), true);
        createMessage(2, 1, 20, "Druga", LocalDateTime.now().minusMinutes(2), false);
        createMessage(1, 3, 20, "Drugi korisnik", LocalDateTime.now().minusMinutes(1), false);

        var response = messageService.getConversation(1, 2, 1, 10);
        var meta = (Map<?, ?>) response.meta();

        assertThat(response.data()).extracting("content").containsExactly("Prva", "Druga");
        assertThat(meta.get("total")).isEqualTo(2L);
    }

    @Test
    void patchMessageUpdatesAllowedFields() throws Exception {
        Message stored = createMessage(1, 2, 20, "Stari tekst", LocalDateTime.now(), false);
        var patch = new ObjectMapper().readTree("""
            [
              { "op": "replace", "path": "/content", "value": "Novi tekst" },
              { "op": "replace", "path": "/isRead", "value": true }
            ]
            """);

        var response = messageService.patchMessage(stored.getId(), 2, patch);

        assertThat(response.content()).isEqualTo("Novi tekst");
        assertThat(response.isRead()).isTrue();
    }

    @Test
    void patchMessageRejectsBlankContentAfterPatch() throws Exception {
        Message stored = createMessage(1, 2, 20, "Stari tekst", LocalDateTime.now(), false);
        var patch = new ObjectMapper().readTree("""
            [
              { "op": "replace", "path": "/content", "value": "" }
            ]
            """);

        assertThatThrownBy(() -> messageService.patchMessage(stored.getId(), 1, patch))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getConversationsByOrderFiltersMessagesByOrderId() {
        createMessage(1, 2, 30, "Narudzba 30", LocalDateTime.now().minusMinutes(2), false);
        createMessage(2, 1, 31, "Narudzba 31", LocalDateTime.now().minusMinutes(1), false);

        var response = messageService.getConversationsByOrder(30, 1, 10);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().orderId()).isEqualTo(30);
        assertThat(response.data().getFirst().content()).isEqualTo("Narudzba 30");
    }

    @Test
    void getConversationsByOrderSupportsSortWhitelist() {
        createMessage(1, 2, 30, "B", LocalDateTime.now().minusMinutes(2), false);
        createMessage(2, 1, 30, "A", LocalDateTime.now().minusMinutes(1), false);

        var response = messageService.getConversationsByOrder(30, 1, 10, "content", "asc");
        var meta = (Map<?, ?>) response.meta();

        assertThat(response.data()).extracting("content").containsExactly("A", "B");
        assertThat(meta.get("sortBy")).isEqualTo("content");
        assertThat(meta.get("direction")).isEqualTo("asc");
    }

    @Test
    void getConversationListSortsByLastMessageAndCountsUnreadMessages() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 4, 12, 0);
        createMessage(1, 2, null, "Stara poslana", base.minusMinutes(3), true);
        createMessage(2, 1, null, "Nova neprocitana", base.minusMinutes(2), false);
        createMessage(3, 1, null, "Najnovija neprocitana", base.minusMinutes(1), false);

        var conversations = messageService.getConversationList(1);

        assertThat(conversations).hasSize(2);
        assertThat(conversations.getFirst().partnerId()).isEqualTo(3);
        assertThat(conversations.getFirst().lastMessage()).isEqualTo("Najnovija neprocitana");
        assertThat(conversations.getFirst().unreadCount()).isEqualTo(1);
        assertThat(conversations.get(1).partnerId()).isEqualTo(2);
        assertThat(conversations.get(1).unreadCount()).isEqualTo(1);
    }

    private Message createMessage(Integer senderId, Integer receiverId, String content, boolean isRead) {
        return createMessage(senderId, receiverId, null, content, LocalDateTime.now(), isRead);
    }

    private Message createMessage(Integer senderId, Integer receiverId, Integer orderId,
                                  String content, LocalDateTime sentAt, boolean isRead) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setOrderId(orderId);
        message.setContent(content);
        message.setSentAt(sentAt);
        message.setIsRead(isRead);
        return messageRepository.save(message);
    }
}
