package com.skillbridge.communication.service;

import com.skillbridge.communication.model.Review;
import com.skillbridge.communication.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public Review create(Integer reviewerId, Integer orderId, Integer revieweeId, int rating, String comment) {
        if (reviewerId.equals(revieweeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot review yourself");
        }
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        Review review = new Review();
        review.setOrderId(orderId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    public Map<String, Object> findByReviewee(Integer revieweeId, int page, int limit) {
        Page<Review> result = reviewRepository.findByRevieweeId(revieweeId, PageRequest.of(page - 1, limit));

        return Map.of(
            "data", result.getContent(),
            "meta", Map.of(
                "total", result.getTotalElements(),
                "page", page,
                "limit", limit,
                "totalPages", result.getTotalPages()
            )
        );
    }

    public List<Review> findByOrder(Integer orderId) {
        return reviewRepository.findByOrderId(orderId);
    }

    public Map<String, Object> getAverageRating(Integer revieweeId) {
        Object[] result = reviewRepository.getAverageRating(revieweeId);
        Object[] row = normalizeAggregateRow(result);

        double avg = 0.0;
        long count = 0;

        if (row != null && row.length >= 2) {
            if (row[0] != null) avg = ((Number) row[0]).doubleValue();
            if (row[1] != null) count = ((Number) row[1]).longValue();
        }

        double rounded = BigDecimal.valueOf(avg)
            .setScale(1, RoundingMode.HALF_UP)
            .doubleValue();

        return Map.of(
            "averageRating", rounded,
            "totalReviews", count
        );
    }

    private Object[] normalizeAggregateRow(Object[] result) {
        if (result != null && result.length == 1 && result[0] instanceof Object[] nested) {
            return nested;
        }
        return result;
    }
}
