package com.skillbridge.user.service;

import com.skillbridge.user.dto.UpdateUserRequest;
import com.skillbridge.user.dto.UserResponse;
import com.skillbridge.user.model.User;
import com.skillbridge.user.model.UserRole;
import com.skillbridge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user = new User();
        user.setUsername("service.user");
        user.setEmail("service@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.CLIENT);
        user.setCountry("Bosna i Hercegovina");
        user = userRepository.save(user);
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        UserResponse response = userService.update(
            user.getId(),
            new UpdateUserRequest("Updated", null, "New bio", null, null)
        );

        assertThat(response.firstName()).isEqualTo("Updated");
        assertThat(response.bio()).isEqualTo("New bio");
        assertThat(response.country()).isEqualTo("Bosna i Hercegovina");
    }

    @Test
    void findByIdThrowsNotFoundForMissingUser() {
        assertThatThrownBy(() -> userService.findById(9999))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
