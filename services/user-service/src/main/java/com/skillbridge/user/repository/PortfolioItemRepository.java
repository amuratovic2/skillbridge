package com.skillbridge.user.repository;

import com.skillbridge.user.model.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Integer> {
    List<PortfolioItem> findByUser_IdOrderByCreatedAtDesc(Integer userId);

    @Query("""
        select p
        from PortfolioItem p
        join p.user u
        where u.id = :userId
          and u.isActive = true
        order by p.createdAt desc
        """)
    List<PortfolioItem> findForActiveUser(@Param("userId") Integer userId);
}
