package com.skillbridge.communication.service;

import com.skillbridge.communication.dto.ConversationSummaryResponse;
import com.skillbridge.communication.dto.MessageResponse;
import com.skillbridge.communication.dto.PageResponse;
import com.skillbridge.communication.dto.SendMessageRequest;
import com.skillbridge.communication.mapper.MessageMapper;
import com.skillbridge.communication.model.Message;
import com.skillbridge.communication.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public MessageResponse send(Integer senderId, SendMessageRequest request) {
        if (senderId.equals(request.receiverId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send message to yourself");
        }

        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(request.receiverId());
        msg.setOrderId(request.orderId());
        msg.setContent(request.content().trim());
        msg.setSentAt(LocalDateTime.now());
        return MessageMapper.toResponse(messageRepository.save(msg));
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getConversation(Integer userId, Integer otherUserId, int page, int limit) {
        Page<Message> result = messageRepository.findConversation(
            userId, otherUserId, PageRequest.of(page - 1, limit)
        );

        List<Message> messages = new ArrayList<>(result.getContent());
        Collections.reverse(messages);

        return new PageResponse<>(
            messages.stream().map(MessageMapper::toResponse).toList(),
            Map.of(
                "total", result.getTotalElements(),
                "page", page,
                "limit", limit,
                "totalPages", result.getTotalPages()
            )
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getConversationsByOrder(Integer orderId, int page, int limit) {
        Page<Message> result = messageRepository.findByOrderId(orderId, PageRequest.of(page - 1, limit));

        return new PageResponse<>(
            result.getContent().stream().map(MessageMapper::toResponse).toList(),
            Map.of(
                "total", result.getTotalElements(),
                "page", page,
                "limit", limit,
                "totalPages", result.getTotalPages()
            )
        );
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversationList(Integer userId) {
        List<Object[]> rows = messageRepository.findConversationList(userId);
        List<ConversationSummaryResponse> conversations = new ArrayList<>();

        for (Object[] row : rows) {
            conversations.add(new ConversationSummaryResponse(
                ((Number) row[0]).intValue(),
                (String) row[1],
                asLocalDateTime(row[2]),
                ((Number) row[3]).intValue()
            ));
        }

        return conversations;
    }

    @Transactional
    public int markAsRead(Integer userId, Integer senderId) {
        return messageRepository.markAsRead(userId, senderId);
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw new IllegalArgumentException("Unsupported timestamp value");
    }
}
