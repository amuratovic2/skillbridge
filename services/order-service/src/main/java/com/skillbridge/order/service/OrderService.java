package com.skillbridge.order.service;

import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderHistory;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final Map<OrderStatus, List<OrderStatus>> VALID_TRANSITIONS = Map.of(
        OrderStatus.PENDING, List.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED),
        OrderStatus.ACCEPTED, List.of(OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED),
        OrderStatus.IN_PROGRESS, List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.DISPUTED),
        OrderStatus.DELIVERED, List.of(OrderStatus.COMPLETED, OrderStatus.REVISION_REQUESTED, OrderStatus.DISPUTED),
        OrderStatus.REVISION_REQUESTED, List.of(OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED, OrderStatus.DISPUTED),
        OrderStatus.COMPLETED, List.of(),
        OrderStatus.CANCELLED, List.of(),
        OrderStatus.DISPUTED, List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED)
    );

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order create(Integer clientId, Integer gigId, BigDecimal totalCost, int maxRevisions, int deliveryDays) {
        Order order = new Order();
        order.setClientId(clientId);
        order.setGigId(gigId);
        order.setTotalCost(totalCost);
        order.setMaxRevisions(maxRevisions);
        order.setDeliveryDeadline(LocalDateTime.now().plusDays(deliveryDays));

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setChangedByUserId(clientId.longValue());
        history.setActionType("ORDER_CREATED");
        history.setNewStatus(OrderStatus.PENDING.name());
        order.getHistory().add(history);

        return orderRepository.save(order);
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    public Map<String, Object> findByClient(Integer clientId, int page, int limit) {
        Page<Order> orderPage = orderRepository.findByClientIdOrderByOrderDateDesc(
            clientId, PageRequest.of(page - 1, limit)
        );
        return buildPageResult(orderPage, page, limit);
    }

    public Map<String, Object> findBySeller(Integer sellerId, int page, int limit) {
        Page<Order> orderPage = orderRepository.findAll(PageRequest.of(page - 1, limit));
        return buildPageResult(orderPage, page, limit);
    }

    @Transactional
    public Order updateStatus(Long orderId, Integer userId, OrderStatus newStatus, String note) {
        Order order = findById(orderId);
        OrderStatus oldStatus = order.getStatus();

        List<OrderStatus> allowed = VALID_TRANSITIONS.get(oldStatus);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid status transition from " + oldStatus + " to " + newStatus);
        }

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        } else if (newStatus == OrderStatus.CANCELLED) {
            order.setCancelledAt(LocalDateTime.now());
        }

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setChangedByUserId(userId.longValue());
        history.setActionType("STATUS_CHANGE");
        history.setOldStatus(oldStatus.name());
        history.setNewStatus(newStatus.name());
        history.setNote(note);
        order.getHistory().add(history);

        return orderRepository.save(order);
    }

    @Transactional
    public Order requestRevision(Long orderId, Integer clientId, String message) {
        Order order = findById(orderId);

        if (!order.getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the client can request a revision");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only request revision on a delivered order");
        }
        if (order.getUsedRevisions() >= order.getMaxRevisions()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum number of revisions reached");
        }

        order.setUsedRevisions(order.getUsedRevisions() + 1);
        order.setStatus(OrderStatus.REVISION_REQUESTED);

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setChangedByUserId(clientId.longValue());
        history.setActionType("REVISION_REQUESTED");
        history.setOldStatus(OrderStatus.DELIVERED.name());
        history.setNewStatus(OrderStatus.REVISION_REQUESTED.name());
        history.setNote(message);
        order.getHistory().add(history);

        return orderRepository.save(order);
    }

    private Map<String, Object> buildPageResult(Page<Order> page, int pageNum, int limit) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", page.getContent());
        result.put("meta", Map.of(
            "total", page.getTotalElements(),
            "page", pageNum,
            "limit", limit,
            "totalPages", page.getTotalPages()
        ));
        return result;
    }
}
