package com.skillbridge.order.service;

import com.skillbridge.order.model.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class OrderAccess {

    private OrderAccess() {
    }

    static void requireParticipantOrAdmin(Order order, Integer userId, String userRole) {
        if (isAdmin(userRole) || isParticipant(order, userId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nemate pristup ovoj narudzbi");
    }

    static void requireSeller(Order order, Integer userId) {
        if (order.getSellerId() != null && order.getSellerId().equals(userId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Samo freelancer ove narudzbe moze izvrsiti akciju");
    }

    static void requireAdmin(String userRole) {
        if (isAdmin(userRole)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Samo administrator moze izvrsiti ovu akciju");
    }

    private static boolean isParticipant(Order order, Integer userId) {
        return userId != null && (userId.equals(order.getClientId()) || userId.equals(order.getSellerId()));
    }

    private static boolean isAdmin(String userRole) {
        return "ADMIN".equalsIgnoreCase(userRole);
    }
}
