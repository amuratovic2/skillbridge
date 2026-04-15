package com.skillbridge.user.repository;

import com.skillbridge.user.model.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Integer> {
    List<PortfolioItem> findByUser_IdOrderByCreatedAtDesc(Integer userId);
}
