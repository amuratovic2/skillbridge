package com.skillbridge.gig.controller;

import com.skillbridge.gig.dto.ApiResponse;
import com.skillbridge.gig.dto.CreateGigRequest;
import com.skillbridge.gig.dto.UpdateGigRequest;
import com.skillbridge.gig.model.Gig;
import com.skillbridge.gig.service.GigService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gigs")
public class GigController {

    private final GigService gigService;

    public GigController(GigService gigService) {
        this.gigService = gigService;
    }

    @PostMapping
    public ApiResponse<Gig> create(
            @RequestHeader("x-user-id") String userId,
            @RequestBody CreateGigRequest body) {
        Gig result = gigService.create(Integer.parseInt(userId), body);
        return ApiResponse.ok(result);
    }

    @GetMapping("/search")
    public ApiResponse<?> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer deliveryTime,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit) {
        Map<String, Object> result = gigService.search(q, categoryId, minPrice, maxPrice, deliveryTime, sortBy, page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/featured")
    public ApiResponse<List<Gig>> getFeatured(@RequestParam(defaultValue = "6") int limit) {
        List<Gig> result = gigService.getFeatured(limit);
        return ApiResponse.ok(result);
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ApiResponse<List<Gig>> findByFreelancer(@PathVariable Integer freelancerId) {
        List<Gig> result = gigService.findByFreelancerId(freelancerId);
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<Gig> findById(@PathVariable Integer id) {
        Gig result = gigService.findById(id);
        return ApiResponse.ok(result);
    }

    @PatchMapping("/{id}")
    public ApiResponse<Gig> update(
            @PathVariable Integer id,
            @RequestHeader("x-user-id") String userId,
            @RequestBody UpdateGigRequest body) {
        Gig result = gigService.update(id, Integer.parseInt(userId), body);
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(
            @PathVariable Integer id,
            @RequestHeader("x-user-id") String userId) {
        Map<String, String> result = gigService.delete(id, Integer.parseInt(userId));
        return ApiResponse.ok(result);
    }
}
