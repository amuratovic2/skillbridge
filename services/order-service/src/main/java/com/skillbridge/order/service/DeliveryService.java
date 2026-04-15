package com.skillbridge.order.service;

import com.skillbridge.order.model.Delivery;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderHistory;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.repository.DeliveryRepository;
import com.skillbridge.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    public DeliveryService(DeliveryRepository deliveryRepository, OrderRepository orderRepository) {
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Delivery create(Long orderId, Integer freelancerId, String message, String fileUrl, String fileName) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        List<Delivery> existing = deliveryRepository.findByOrderIdOrderByVersionNumberDesc(orderId);
        int nextVersion = existing.isEmpty() ? 1 : existing.get(0).getVersionNumber() + 1;

        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setVersionNumber(nextVersion);
        delivery.setMessage(message);
        delivery.setFileUrl(fileUrl);
        delivery.setFileName(fileName);

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.DELIVERED);

        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setChangedByUserId(freelancerId.longValue());
        history.setActionType("DELIVERY_SUBMITTED");
        history.setOldStatus(oldStatus.name());
        history.setNewStatus(OrderStatus.DELIVERED.name());
        history.setNote("Delivery v" + nextVersion);
        order.getHistory().add(history);

        orderRepository.save(order);
        return deliveryRepository.save(delivery);
    }

    public List<Delivery> findByOrderId(Long orderId) {
        return deliveryRepository.findByOrderIdOrderByVersionNumberDesc(orderId);
    }

    public Optional<Delivery> findByVersion(Long orderId, int versionNumber) {
        return deliveryRepository.findByOrderIdAndVersionNumber(orderId, versionNumber);
    }
}
