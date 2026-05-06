package com.skillbridge.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.skillbridge.order.dto.OrderPatchState;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
public class OrderPatchService {

    private static final Set<String> PATCHABLE_FIELDS = Set.of("totalCost", "maxRevisions");

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public OrderPatchService(OrderRepository orderRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order patch(Long orderId, JsonPatch patch) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Narudžba nije pronađena"));

        validatePatchFields(patch);

        try {
            OrderPatchState currentState = new OrderPatchState();
            currentState.setTotalCost(order.getTotalCost());
            currentState.setMaxRevisions(order.getMaxRevisions());

            JsonNode patched = patch.apply(objectMapper.convertValue(currentState, JsonNode.class));
            OrderPatchState patchedState = objectMapper.treeToValue(patched, OrderPatchState.class);

            if (patchedState.getTotalCost() != null) {
                if (patchedState.getTotalCost().signum() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "totalCost mora biti pozitivan");
                }
                order.setTotalCost(patchedState.getTotalCost());
            }
            if (patchedState.getMaxRevisions() != null) {
                if (patchedState.getMaxRevisions() < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxRevisions ne može biti negativan");
                }
                order.setMaxRevisions(patchedState.getMaxRevisions());
            }

            return orderRepository.save(order);
        } catch (JsonPatchException | JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neispravan JSON Patch: " + e.getMessage());
        }
    }

    private void validatePatchFields(JsonPatch patch) {
        String patchStr = patch.toString();
        if (patchStr.contains("/status") || patchStr.contains("/clientId") || patchStr.contains("/gigId")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Nije dozvoljeno mijenjati: status, clientId, gigId putem PATCH");
        }
    }
}