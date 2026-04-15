package com.skillbridge.gig.repository;

import com.skillbridge.gig.model.GigImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GigImageRepository extends JpaRepository<GigImage, Integer> {

    List<GigImage> findByGigIdOrderBySortOrder(Integer gigId);
}
