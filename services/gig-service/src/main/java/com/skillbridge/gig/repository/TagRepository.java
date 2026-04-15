package com.skillbridge.gig.repository;

import com.skillbridge.gig.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByName(String name);

    List<Tag> findAllByOrderByNameAsc();

    @Query(value = """
        SELECT t.id, t.name, t.slug, COUNT(gt.gig_id) AS gig_count
        FROM gigs.tags t
        LEFT JOIN gigs.gig_tags gt ON t.id = gt.tag_id
        GROUP BY t.id, t.name, t.slug
        ORDER BY gig_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findPopularRaw(int limit);
}
