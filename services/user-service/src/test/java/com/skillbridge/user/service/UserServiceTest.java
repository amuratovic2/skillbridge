package com.skillbridge.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.skillbridge.user.dto.UpdateUserRequest;
import com.skillbridge.user.dto.UserResponse;
import com.skillbridge.user.model.User;
import com.skillbridge.user.model.UserRole;
import com.skillbridge.user.repository.UserRepository;
import jakarta.validation.ConstraintViolationException;
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
    private ObjectMapper objectMapper;

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
    void findAllAppliesSearchAndSortParameters() {
        User second = new User();
        second.setUsername("zzz.user");
        second.setEmail("zzz@example.com");
        second.setPasswordHash(passwordEncoder.encode("password123"));
        second.setRole(UserRole.FREELANCER);
        second.setCountry("Bosna i Hercegovina");
        userRepository.save(second);

        var response = userService.findAll(
            1,
            10,
            "username",
            "desc",
            "user",
            null,
            "Bosna i Hercegovina",
            null
        );

        assertThat(response.data()).extracting(UserResponse::username)
            .containsExactly("zzz.user", "service.user");
    }

    @Test
    void patchAppliesJsonPatchToAllowedProfileFields() throws Exception {
        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree("""
            [
              {"op": "replace", "path": "/country", "value": "Germany"}
            ]
            """));

        UserResponse response = userService.patch(user.getId(), patch);

        assertThat(response.country()).isEqualTo("Germany");
    }

    @Test
    void patchValidatesResultingProfile() throws Exception {
        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree("""
            [
              {"op": "replace", "path": "/country", "value": "%s"}
            ]
            """.formatted("x".repeat(101))));

        assertThatThrownBy(() -> userService.patch(user.getId(), patch))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void findByIdThrowsNotFoundForMissingUser() {
        assertThatThrownBy(() -> userService.findById(9999))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
