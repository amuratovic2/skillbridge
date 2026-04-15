package com.skillbridge.user.repository;

import com.skillbridge.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailOrUsername(String email, String username);
    Page<User> findByIsActiveTrue(Pageable pageable);
    Optional<User> findByIdAndIsActiveTrue(Integer id);
}
