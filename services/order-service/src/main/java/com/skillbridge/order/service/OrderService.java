package com.skillbridge.order.service;

import com.skillbridge.order.client.GigClient;
import com.skillbridge.order.config.RabbitMQConfig;
import com.skillbridge.order.dto.CreateOrderRequest;
import com.skillbridge.order.dto.GigDto;
import com.skillbridge.order.events.OrderEvent;
import com.skillbridge.order.events.OrderEventPublisher;
import com.skillbridge.order.mapper.OrderMapper;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderHistory;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.repository.OrderRepository;
import com.skillbridge.order.saga.OrderPlacedEvent;
import com.skillbridge.order.saga.OrderSagaPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final GigClient gigClient;
    private final OrderSagaPublisher sagaPublisher;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, GigClient gigClient,
                        OrderSagaPublisher sagaPublisher,
                        OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.gigClient = gigClient;
        this.sagaPublisher = sagaPublisher;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates an order by validating the gig via gig-service (synchronous call).
     * Price, seller, revision count and delivery deadline are sourced from the gig —
     * clients cannot manipulate these fields.
     */
    @Transactional
    public Order create(Integer clientId, Integer gigId) {
        GigDto gig = gigClient.getGig(gigId);
        validateGigIsActive(gig);

        if (clientId.equals(gig.getFreelancerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ne možete naručiti vlastiti gig");
        }

        Order order = new Order();
        order.setClientId(clientId);
        order.setGigId(gigId);
        order.setSellerId(gig.getFreelancerId());
        order.setTotalCost(gig.getCost());
        order.setMaxRevisions(gig.getRevisionCount());
        order.setDeliveryDeadline(LocalDateTime.now().plusDays(gig.getDeliveryTime()));

        addHistory(order, clientId.longValue(), "ORDER_CREATED", null, OrderStatus.PENDING.name(), null);
        Order saved = orderRepository.save(order);

        // Publish async saga event – gig-service will validate gig availability
        // and reply with order.confirmed or order.rejected
        sagaPublisher.publishOrderPlaced(new OrderPlacedEvent(
            saved.getId(), gigId, clientId, gig.getFreelancerId(), gig.getCost()
        ));

        // Notify the freelancer that a new order is awaiting confirmation
        eventPublisher.publishOrderEvent(OrderEvent.of(
            RabbitMQConfig.ORDER_PLACED_KEY, saved.getId(), clientId,
            gig.getFreelancerId(), gigId, null, OrderStatus.PENDING.name(),
            saved.getTotalCost(), clientId, null
        ));

        return saved;
    }

    @Transactional
    public List<Order> batchCreate(Integer clientId, List<CreateOrderRequest> requests) {
        List<Order> orders = requests.stream().map(req -> {
            GigDto gig = gigClient.getGig(req.getGigId());
            validateGigIsActive(gig);

            Order order = new Order();
            order.setClientId(clientId);
            order.setGigId(req.getGigId());
            order.setSellerId(gig.getFreelancerId());
            order.setTotalCost(gig.getCost());
            order.setMaxRevisions(gig.getRevisionCount());
            order.setDeliveryDeadline(LocalDateTime.now().plusDays(gig.getDeliveryTime()));
            addHistory(order, clientId.longValue(), "ORDER_CREATED", null, OrderStatus.PENDING.name(), null);
            return order;
        }).toList();

        return orderRepository.saveAll(orders);
    }

    public Order findById(Long id) {
        return orderRepository.findWithDetailsById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Narudžba nije pronađena"));
    }

    public Map<String, Object> findByClient(Integer clientId, int page, int limit, String sortBy, String sortDir) {
        Sort sort = buildSort(sortBy, sortDir);
        Page<Order> orderPage = orderRepository.findByClientIdOrderByOrderDateDesc(
            clientId, PageRequest.of(page - 1, limit, sort)
        );
        return buildPageResult(orderPage, page, limit);
    }

    public Map<String, Object> findBySeller(Integer sellerId, int page, int limit, String sortBy, String sortDir) {
        Sort sort = buildSort(sortBy, sortDir);
        Page<Order> orderPage = orderRepository.findBySellerIdOrderByOrderDateDesc(
            sellerId, PageRequest.of(page - 1, limit, sort)
        );
        return buildPageResult(orderPage, page, limit);
    }

    public List<Order> findByClientAndStatus(Integer clientId, OrderStatus status) {
        return orderRepository.findByClientIdAndStatus(clientId, status);
    }

    public List<Order> findOverdue() {
        return orderRepository.findOverdueOrders(LocalDateTime.now());
    }

    public BigDecimal getTotalRevenue(Integer clientId) {
        return orderRepository.sumCompletedRevenueByClient(clientId);
    }

    public Map<String, Long> getStatusStatistics() {
        Map<String, Long> stats = new HashMap<>();
        List<Object[]> rawStats = orderRepository.countByStatusNative();
        for (Object[] row : rawStats) {
            stats.put((String) row[0], ((Number) row[1]).longValue());
        }
        return stats;
    }

    @Transactional
    public Order updateStatus(Long orderId, Integer userId, String userRole, OrderStatus newStatus, String note) {
        Order order = findById(orderId);

        boolean isAdmin = "ADMIN".equals(userRole);
        boolean isParty = userId.equals(order.getClientId()) || userId.equals(order.getSellerId());
        if (!isAdmin && !isParty) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Nemate pristup ovoj narudžbi");
        }

        OrderStatus oldStatus = order.getStatus();

        List<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(oldStatus, List.of());
        if (!allowed.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Neispravan prijelaz statusa: " + oldStatus + " → " + newStatus);
        }

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.COMPLETED) order.setCompletedAt(LocalDateTime.now());
        else if (newStatus == OrderStatus.CANCELLED) order.setCancelledAt(LocalDateTime.now());

        addHistory(order, userId.longValue(), "STATUS_CHANGE", oldStatus.name(), newStatus.name(), note);
        Order saved = orderRepository.save(order);

        String routingKey = routingKeyFor(newStatus);
        if (routingKey != null) {
            eventPublisher.publishOrderEvent(OrderEvent.of(
                routingKey, saved.getId(), saved.getClientId(), saved.getSellerId(),
                saved.getGigId(), oldStatus.name(), newStatus.name(),
                saved.getTotalCost(), userId, note
            ));
        }

        return saved;
    }

    private String routingKeyFor(OrderStatus status) {
        return switch (status) {
            case ACCEPTED -> RabbitMQConfig.ORDER_ACCEPTED_KEY;
            case IN_PROGRESS -> RabbitMQConfig.ORDER_IN_PROGRESS_KEY;
            case DELIVERED -> RabbitMQConfig.ORDER_DELIVERED_KEY;
            case COMPLETED -> RabbitMQConfig.ORDER_COMPLETED_KEY;
            case CANCELLED -> RabbitMQConfig.ORDER_CANCELLED_KEY;
            case REVISION_REQUESTED -> RabbitMQConfig.ORDER_REVISION_REQUESTED_KEY;
            default -> null;
        };
    }

    @Transactional
    public Order requestRevision(Long orderId, Integer clientId, String message) {
        Order order = findById(orderId);

        if (!order.getClientId().equals(clientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Samo klijent može zatražiti reviziju");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revizija se može zatražiti samo za dostavljenu narudžbu");
        }
        if (order.getUsedRevisions() >= order.getMaxRevisions()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dostignut maksimalni broj revizija");
        }

        order.setUsedRevisions(order.getUsedRevisions() + 1);
        order.setStatus(OrderStatus.REVISION_REQUESTED);
        addHistory(order, clientId.longValue(), "REVISION_REQUESTED",
            OrderStatus.DELIVERED.name(), OrderStatus.REVISION_REQUESTED.name(), message);

        Order saved = orderRepository.save(order);

        eventPublisher.publishOrderEvent(OrderEvent.of(
            RabbitMQConfig.ORDER_REVISION_REQUESTED_KEY, saved.getId(),
            saved.getClientId(), saved.getSellerId(), saved.getGigId(),
            OrderStatus.DELIVERED.name(), OrderStatus.REVISION_REQUESTED.name(),
            saved.getTotalCost(), clientId, message
        ));

        return saved;
    }

    private void validateGigIsActive(GigDto gig) {
        if (!"ACTIVE".equals(gig.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Gig nije dostupan za narudžbu (status: " + gig.getStatus() + ")");
        }
    }

    private void addHistory(Order order, Long userId, String action, String oldStatus, String newStatus, String note) {
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setChangedByUserId(userId);
        history.setActionType(action);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setNote(note);
        order.getHistory().add(history);
    }

    private Sort buildSort(String sortBy, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    private Map<String, Object> buildPageResult(Page<Order> page, int pageNum, int limit) {
        return Map.of(
            "data", page.getContent().stream().map(OrderMapper::toDTO).toList(),
            "meta", Map.of(
                "total", page.getTotalElements(),
                "page", pageNum,
                "limit", limit,
                "totalPages", page.getTotalPages()
            )
        );
    }
}
