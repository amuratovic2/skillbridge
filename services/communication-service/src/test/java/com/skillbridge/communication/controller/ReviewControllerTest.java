package com.skillbridge.communication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.communication.model.Review;
import com.skillbridge.communication.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewRepository reviewRepository;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
    }

    @Test
    void createStoresValidReview() throws Exception {
        Map<String, Object> body = Map.of(
            "orderId", 20,
            "revieweeId", 2,
            "rating", 5,
            "comment", "Odlicna saradnja"
        );

        mockMvc.perform(post("/reviews")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andExpect(jsonPath("$.data.reviewerId").value(1))
            .andExpect(jsonPath("$.data.revieweeId").value(2))
            .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    void createRejectsInvalidRating() throws Exception {
        Map<String, Object> body = Map.of(
            "orderId", 20,
            "revieweeId", 2,
            "rating", 6
        );

        mockMvc.perform(post("/reviews")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("rating")));
    }

    @Test
    void createRejectsSelfReview() throws Exception {
        Map<String, Object> body = Map.of(
            "orderId", 20,
            "revieweeId", 1,
            "rating", 5
        );

        mockMvc.perform(post("/reviews")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Cannot review yourself"));
    }

    @Test
    void findByUserReturnsPagedReviews() throws Exception {
        createReview(10, 1, 2, 4, "Prva", LocalDateTime.now().minusMinutes(2));
        createReview(11, 3, 2, 5, "Druga", LocalDateTime.now().minusMinutes(1));
        createReview(12, 4, 5, 3, "Drugi korisnik", LocalDateTime.now());

        mockMvc.perform(get("/reviews/user/2")
                .param("page", "1")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.meta.total").value(2));
    }

    @Test
    void findByOrderReturnsOrderReviews() throws Exception {
        createReview(10, 1, 2, 4, "Prva", LocalDateTime.now().minusMinutes(2));
        createReview(11, 3, 2, 5, "Druga", LocalDateTime.now().minusMinutes(1));

        mockMvc.perform(get("/reviews/order/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].orderId").value(10));
    }

    @Test
    void getAverageRatingReturnsRoundedAverageAndCount() throws Exception {
        createReview(10, 1, 2, 4, "Prva", LocalDateTime.now().minusMinutes(2));
        createReview(11, 3, 2, 5, "Druga", LocalDateTime.now().minusMinutes(1));

        mockMvc.perform(get("/reviews/rating/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.averageRating").value(4.5))
            .andExpect(jsonPath("$.data.totalReviews").value(2));
    }

    private Review createReview(Integer orderId, Integer reviewerId, Integer revieweeId,
                                int rating, String comment, LocalDateTime createdAt) {
        Review review = new Review();
        review.setOrderId(orderId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(createdAt);
        return reviewRepository.save(review);
    }
}
