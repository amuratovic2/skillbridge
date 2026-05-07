package com.skillbridge.user.repository;

import com.skillbridge.user.model.User;
import com.skillbridge.user.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserSearchRepository {
    Page<User> searchActiveUsers(
        String query,
        UserRole role,
        String country,
        String skill,
        Pageable pageable
    );
}
