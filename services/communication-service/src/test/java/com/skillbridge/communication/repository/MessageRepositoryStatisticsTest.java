package com.skillbridge.communication.repository;

import com.skillbridge.communication.model.Message;
import com.skillbridge.communication.service.MessageService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MessageRepositoryStatisticsTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        createMessage(1, 2, "Jedan", LocalDateTime.now().minusMinutes(3));
        createMessage(2, 1, "Dva", LocalDateTime.now().minusMinutes(2));
        createMessage(1, 2, "Tri", LocalDateTime.now().minusMinutes(1));
    }

    @Test
    void getConversationDoesNotTriggerNPlusOneQueries() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var response = messageService.getConversation(1, 2, 1, 10);

        assertThat(response.data()).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    private Message createMessage(Integer senderId, Integer receiverId, String content, LocalDateTime sentAt) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setSentAt(sentAt);
        return messageRepository.save(message);
    }
}
