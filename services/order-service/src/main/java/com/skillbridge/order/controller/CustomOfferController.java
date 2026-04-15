package com.skillbridge.order.controller;

import com.skillbridge.order.dto.ApiResponse;
import com.skillbridge.order.model.CustomOffer;
import com.skillbridge.order.model.CustomOfferStatus;
import com.skillbridge.order.service.CustomOfferService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/custom-offers")
public class CustomOfferController {

    private final CustomOfferService customOfferService;

    public CustomOfferController(CustomOfferService customOfferService) {
        this.customOfferService = customOfferService;
    }

    @PostMapping
    public ApiResponse<?> create(
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody Map<String, Object> body
    ) {
        CustomOffer offer = new CustomOffer();
        if (body.get("gigId") != null) {
            offer.setGigId((Integer) body.get("gigId"));
        }
        offer.setReceiverId((Integer) body.get("receiverId"));
        offer.setTitle((String) body.get("title"));
        offer.setDescription((String) body.get("description"));
        offer.setPrice(new BigDecimal(body.get("price").toString()));
        offer.setDeliveryDays((Integer) body.get("deliveryDays"));
        offer.setRevisionCount((Integer) body.get("revisionCount"));
        return ApiResponse.ok(customOfferService.create(userId, offer));
    }

    @GetMapping("/received")
    public ApiResponse<?> getReceived(@RequestHeader("x-user-id") Integer userId) {
        return ApiResponse.ok(customOfferService.findReceived(userId));
    }

    @GetMapping("/sent")
    public ApiResponse<?> getSent(@RequestHeader("x-user-id") Integer userId) {
        return ApiResponse.ok(customOfferService.findSent(userId));
    }

    @PatchMapping("/{id}/respond")
    public ApiResponse<?> respond(
        @PathVariable Long id,
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody Map<String, String> body
    ) {
        CustomOfferStatus status = CustomOfferStatus.valueOf(body.get("status"));
        return ApiResponse.ok(customOfferService.respond(id, userId, status));
    }

    @PatchMapping("/{id}/withdraw")
    public ApiResponse<?> withdraw(
        @PathVariable Long id,
        @RequestHeader("x-user-id") Integer userId
    ) {
        return ApiResponse.ok(customOfferService.withdraw(id, userId));
    }
}
