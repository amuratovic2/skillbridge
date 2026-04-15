package com.skillbridge.communication.repository;

import com.skillbridge.communication.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    Page<Review> findByRevieweeId(Integer revieweeId, Pageable pageable);

    List<Review> findByOrderId(Integer orderId);

    @Query("SELECT COALESCE(AVG(r.rating), 0), COUNT(r) FROM Review r WHERE r.revieweeId = :revieweeId")
    Object[] getAverageRating(@Param("revieweeId") Integer revieweeId);
}
