package com.skillbridge.order.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillbridge.order.dto.ApiResponse;
import com.skillbridge.order.dto.CreateCustomOfferRequest;
import com.skillbridge.order.model.CustomOffer;
import com.skillbridge.order.model.CustomOfferStatus;
import com.skillbridge.order.service.CustomOfferService;

import jakarta.validation.Valid;

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
        @Valid @RequestBody CreateCustomOfferRequest request
    ) {
        CustomOffer offer = new CustomOffer();

        offer.setGigId(request.getGigId());
        offer.setReceiverId(request.getReceiverId());
        offer.setTitle(request.getTitle());
        offer.setDescription(request.getDescription());
        offer.setPrice(request.getPrice());
        offer.setDeliveryDays(request.getDeliveryDays());
        offer.setRevisionCount(request.getRevisionCount());

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
