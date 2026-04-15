package com.skillbridge.communication.service;

import com.skillbridge.communication.model.Message;
import com.skillbridge.communication.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message send(Integer senderId, Integer receiverId, Integer orderId, String content) {
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setOrderId(orderId);
        msg.setContent(content);
        msg.setSentAt(LocalDateTime.now());
        return messageRepository.save(msg);
    }

    public Map<String, Object> getConversation(Integer userId, Integer otherUserId, int page, int limit) {
        Page<Message> result = messageRepository.findConversation(
            userId, otherUserId, PageRequest.of(page - 1, limit)
        );

        List<Message> messages = new ArrayList<>(result.getContent());
        Collections.reverse(messages);

        return Map.of(
            "data", messages,
            "meta", Map.of(
                "total", result.getTotalElements(),
                "page", page,
                "limit", limit,
                "totalPages", result.getTotalPages()
            )
        );
    }

    public Map<String, Object> getConversationsByOrder(Integer orderId, int page, int limit) {
        Page<Message> result = messageRepository.findByOrderId(orderId, PageRequest.of(page - 1, limit));

        return Map.of(
            "data", result.getContent(),
            "meta", Map.of(
                "total", result.getTotalElements(),
                "page", page,
                "limit", limit,
                "totalPages", result.getTotalPages()
            )
        );
    }

    public List<Map<String, Object>> getConversationList(Integer userId) {
        List<Object[]> rows = messageRepository.findConversationList(userId);
        List<Map<String, Object>> conversations = new ArrayList<>();

        for (Object[] row : rows) {
            Map<String, Object> conv = new LinkedHashMap<>();
            conv.put("partnerId", row[0]);
            conv.put("lastMessage", row[1]);
            conv.put("lastAt", row[2]);
            conv.put("unreadCount", ((Number) row[3]).intValue());
            conversations.add(conv);
        }

        return conversations;
    }

    @Transactional
    public int markAsRead(Integer userId, Integer senderId) {
        return messageRepository.markAsRead(userId, senderId);
    }
}
