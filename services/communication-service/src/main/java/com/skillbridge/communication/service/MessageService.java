package com.skillbridge.communication.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.skillbridge.communication.dto.BatchSendMessagesRequest;
import com.skillbridge.communication.dto.ConversationSummaryResponse;
import com.skillbridge.communication.dto.MessagePatchState;
import com.skillbridge.communication.dto.MessageResponse;
import com.skillbridge.communication.dto.PageResponse;
import com.skillbridge.communication.dto.RemoteUserProfile;
import com.skillbridge.communication.dto.SendMessageRequest;
import com.skillbridge.communication.mapper.MessageMapper;
import com.skillbridge.communication.messaging.MessageEventPublisher;
import com.skillbridge.communication.messaging.MessageSentEvent;
import com.skillbridge.communication.model.Message;
import com.skillbridge.communication.repository.MessageRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Map<String, String> MESSAGE_SORTS = Map.of(
        "sentAt", "sentAt",
        "senderId", "senderId",
        "receiverId", "receiverId",
        "content", "content"
    );

    private final MessageRepository messageRepository;
    private final PageableFactory pageableFactory;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final UserDirectoryClient userDirectoryClient;
    private final MessageEventPublisher messageEventPublisher;
    private final boolean userValidationEnabled;

    public MessageService(
        MessageRepository messageRepository,
        PageableFactory pageableFactory,
        ObjectMapper objectMapper,
        Validator validator,
        UserDirectoryClient userDirectoryClient,
        MessageEventPublisher messageEventPublisher,
        @Value("${communication.user-validation.enabled:true}") boolean userValidationEnabled
    ) {
        this.messageRepository = messageRepository;
        this.pageableFactory = pageableFactory;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.userDirectoryClient = userDirectoryClient;
        this.messageEventPublisher = messageEventPublisher;
        this.userValidationEnabled = userValidationEnabled;
    }

    @Transactional
    public MessageResponse send(Integer senderId, SendMessageRequest request) {
        validateParticipants(senderId, List.of(request.receiverId()));
        Message saved = messageRepository.save(toMessage(senderId, request));
        // Two-sided async: every send hops through RabbitMQ so the broker's
        // rate chart spikes on publish AND on the consumer ack — the receiver
        // picks the notification up on the consume side.
        publishMessageSent(saved);
        return MessageMapper.toResponse(saved);
    }

    @Transactional
    public List<MessageResponse> sendBatch(Integer senderId, BatchSendMessagesRequest request) {
        validateParticipants(
            senderId,
            request.messages().stream()
                .map(SendMessageRequest::receiverId)
                .distinct()
                .toList()
        );

        List<Message> messages = request.messages().stream()
            .map(messageRequest -> toMessage(senderId, messageRequest))
            .toList();

        List<Message> saved = messageRepository.saveAll(messages);
        saved.forEach(this::publishMessageSent);
        return saved.stream()
            .map(MessageMapper::toResponse)
            .toList();
    }

    private void publishMessageSent(Message message) {
        String preview = message.getContent() == null ? "" : message.getContent();
        if (preview.length() > 120) {
            preview = preview.substring(0, 117) + "...";
        }
        messageEventPublisher.publishMessageSent(MessageSentEvent.of(
            message.getId(),
            message.getSenderId(),
            message.getReceiverId(),
            message.getOrderId(),
            preview
        ));
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
    public PageResponse<MessageResponse> getConversationsByOrder(Integer orderId, Integer userId, int page, int limit) {
        return getConversationsByOrder(orderId, userId, page, limit, "sentAt", "desc");
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getConversationsByOrder(
        Integer orderId,
        Integer userId,
        int page,
        int limit,
        String sortBy,
        String direction
    ) {
        Pageable pageable = pageableFactory.build(page, limit, sortBy, direction, MESSAGE_SORTS);
        Page<Message> result = messageRepository.findByOrderIdAndParticipant(orderId, userId, pageable);
        return new PageResponse<>(
            result.getContent().stream().map(MessageMapper::toResponse).toList(),
            pageMeta(result, page, limit, sortBy, direction)
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

    @Transactional
    public MessageResponse patchMessage(Integer messageId, Integer userId, JsonNode patchNode) {
        validatePatchDocument(patchNode);

        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!userId.equals(message.getSenderId()) && !userId.equals(message.getReceiverId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Message does not belong to this user");
        }

        MessagePatchState patched = applyPatch(message, patchNode);
        message.setContent(patched.getContent().trim());
        message.setIsRead(patched.getIsRead());
        return MessageMapper.toResponse(messageRepository.save(message));
    }

    private void validateParticipants(Integer senderId, List<Integer> receiverIds) {
        if (!userValidationEnabled) {
            return;
        }

        RemoteUserProfile sender = userDirectoryClient.findActiveUser(senderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sender user does not exist or is inactive"));

        for (Integer receiverId : receiverIds) {
            if (sender.id().equals(receiverId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send message to yourself");
            }
            userDirectoryClient.findActiveUser(receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receiver user does not exist or is inactive"));
        }
    }

    private Message toMessage(Integer senderId, SendMessageRequest request) {
        if (senderId.equals(request.receiverId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send message to yourself");
        }

        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(request.receiverId());
        msg.setOrderId(request.orderId());
        msg.setContent(request.content().trim());
        msg.setSentAt(LocalDateTime.now());
        return msg;
    }

    private MessagePatchState applyPatch(Message message, JsonNode patchNode) {
        try {
            MessagePatchState current = new MessagePatchState(message.getContent(), message.getIsRead());
            JsonNode currentNode = objectMapper.valueToTree(current);
            JsonNode patchedNode = JsonPatch.fromJson(patchNode).apply(currentNode);
            MessagePatchState patched = objectMapper.treeToValue(patchedNode, MessagePatchState.class);
            validatePatchedState(patched);
            return patched;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON Patch document");
        }
    }

    private void validatePatchDocument(JsonNode patchNode) {
        if (patchNode == null || !patchNode.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON Patch body must be an array");
        }

        for (JsonNode operation : patchNode) {
            String op = operation.path("op").asText("");
            String path = operation.path("path").asText("");
            if (!Set.of("add", "replace", "test").contains(op)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only add, replace and test patch operations are supported");
            }
            if (!Set.of("/content", "/isRead").contains(path)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only /content and /isRead can be patched");
            }
        }
    }

    private void validatePatchedState(MessagePatchState patched) {
        Set<ConstraintViolation<MessagePatchState>> violations = validator.validate(patched);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private Map<String, Object> pageMeta(Page<?> result, int page, int limit, String sortBy, String direction) {
        return Map.of(
            "total", result.getTotalElements(),
            "page", page,
            "limit", limit,
            "totalPages", result.getTotalPages(),
            "sortBy", sortBy,
            "direction", direction == null ? "desc" : direction.toLowerCase()
        );
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
