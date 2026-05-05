package com.skillbridge.user.repository;

import com.skillbridge.user.model.User;
import com.skillbridge.user.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailOrUsername(String email, String username);
    Page<User> findByIsActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = "skills")
    Optional<User> findByIdAndIsActiveTrue(Integer id);

    @EntityGraph(attributePaths = "skills")
    @Query("select u from User u where u.id = :id")
    Optional<User> findWithSkillsById(@Param("id") Integer id);

    @Query("""
        select distinct u
        from User u
        left join u.skills s
        where u.isActive = true
          and (:query is null
               or lower(u.username) like lower(concat('%', :query, '%'))
               or lower(u.email) like lower(concat('%', :query, '%'))
               or lower(coalesce(u.firstName, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(u.lastName, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(u.bio, '')) like lower(concat('%', :query, '%')))
          and (:role is null or u.role = :role)
          and (:country is null or lower(coalesce(u.country, '')) = lower(:country))
          and (:skill is null or lower(s.name) = lower(:skill))
        """)
    Page<User> searchActiveUsers(
        @Param("query") String query,
        @Param("role") UserRole role,
        @Param("country") String country,
        @Param("skill") String skill,
        Pageable pageable
    );
}
