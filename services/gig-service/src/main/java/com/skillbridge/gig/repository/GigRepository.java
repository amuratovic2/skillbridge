package com.skillbridge.gig.repository;

import com.skillbridge.gig.model.Gig;
import com.skillbridge.gig.model.GigStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GigRepository extends JpaRepository<Gig, Integer>, JpaSpecificationExecutor<Gig> {

    List<Gig> findByStatusOrderByCreatedAtDesc(GigStatus status);

    List<Gig> findByFreelancerIdAndStatusNotOrderByCreatedAtDesc(Integer freelancerId, GigStatus status);
}
