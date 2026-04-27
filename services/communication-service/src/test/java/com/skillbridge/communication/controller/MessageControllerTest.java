package com.skillbridge.communication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.communication.model.Message;
import com.skillbridge.communication.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
    }

    @Test
    void sendCreatesMessageForValidRequest() throws Exception {
        Map<String, Object> body = Map.of(
            "receiverId", 2,
            "orderId", 10,
            "content", "Zdravo, moze li kratki update za narudzbu?"
        );

        mockMvc.perform(post("/messages")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andExpect(jsonPath("$.data.senderId").value(1))
            .andExpect(jsonPath("$.data.receiverId").value(2))
            .andExpect(jsonPath("$.data.content").value("Zdravo, moze li kratki update za narudzbu?"));
    }

    @Test
    void sendReturnsValidationErrorForBlankContent() throws Exception {
        Map<String, Object> body = Map.of(
            "receiverId", 2,
            "content", ""
        );

        mockMvc.perform(post("/messages")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("content")));
    }

    @Test
    void sendRejectsMessageToSelf() throws Exception {
        Map<String, Object> body = Map.of(
            "receiverId", 1,
            "content", "Ovo ne smije proci"
        );

        mockMvc.perform(post("/messages")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("bad_request"))
            .andExpect(jsonPath("$.message").value("Cannot send message to yourself"));
    }

    @Test
    void sendRequiresUserHeader() throws Exception {
        Map<String, Object> body = Map.of(
            "receiverId", 2,
            "content", "Poruka bez headera"
        );

        mockMvc.perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("bad_request"))
            .andExpect(jsonPath("$.message", containsString("x-user-id")));
    }

    @Test
    void getConversationReturnsPagedMessagesOldestFirst() throws Exception {
        createMessage(1, 2, 10, "Prva poruka", LocalDateTime.now().minusMinutes(2), true);
        createMessage(2, 1, 10, "Druga poruka", LocalDateTime.now().minusMinutes(1), false);

        mockMvc.perform(get("/messages/conversation/2")
                .header("x-user-id", 1)
                .param("page", "1")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].content").value("Prva poruka"))
            .andExpect(jsonPath("$.data[1].content").value("Druga poruka"))
            .andExpect(jsonPath("$.meta.total").value(2));
    }

    @Test
    void getConversationRejectsInvalidLimit() throws Exception {
        mockMvc.perform(get("/messages/conversation/2")
                .header("x-user-id", 1)
                .param("page", "1")
                .param("limit", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("limit")));
    }

    @Test
    void getConversationsReturnsLatestMessageAndUnreadCount() throws Exception {
        createMessage(1, 2, null, "Starija poruka za korisnika 2", LocalDateTime.now().minusMinutes(5), true);
        createMessage(2, 1, null, "Nova poruka za korisnika 2", LocalDateTime.now().minusMinutes(3), false);
        createMessage(3, 1, null, "Najnovija poruka za korisnika 3", LocalDateTime.now().minusMinutes(1), false);

        mockMvc.perform(get("/messages/conversations")
                .header("x-user-id", 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].partnerId").value(3))
            .andExpect(jsonPath("$.data[0].lastMessage").value("Najnovija poruka za korisnika 3"))
            .andExpect(jsonPath("$.data[0].unreadCount").value(1))
            .andExpect(jsonPath("$.data[1].partnerId").value(2))
            .andExpect(jsonPath("$.data[1].lastMessage").value("Nova poruka za korisnika 2"))
            .andExpect(jsonPath("$.data[1].unreadCount").value(1));
    }

    @Test
    void getByOrderReturnsOnlyMessagesForRequestedOrder() throws Exception {
        createMessage(1, 2, 10, "Poruka za narudzbu 10", LocalDateTime.now().minusMinutes(2), true);
        createMessage(2, 1, 11, "Poruka za narudzbu 11", LocalDateTime.now().minusMinutes(1), false);

        mockMvc.perform(get("/messages/order/10")
                .param("page", "1")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].orderId").value(10))
            .andExpect(jsonPath("$.data[0].content").value("Poruka za narudzbu 10"))
            .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    void markAsReadUpdatesUnreadMessagesFromSender() throws Exception {
        createMessage(2, 1, 10, "Neprocitana poruka", LocalDateTime.now(), false);

        mockMvc.perform(patch("/messages/read/2")
                .header("x-user-id", 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.updated").value(1));
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
