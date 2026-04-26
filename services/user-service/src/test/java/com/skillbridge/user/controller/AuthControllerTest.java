package com.skillbridge.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.user.model.User;
import com.skillbridge.user.model.UserRole;
import com.skillbridge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerReturnsTokensForValidRequest() throws Exception {
        Map<String, String> body = Map.of(
            "username", "new.client",
            "email", "new.client@example.com",
            "password", "password123",
            "role", "CLIENT",
            "firstName", "New",
            "lastName", "Client"
        );

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken", notNullValue()))
            .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
            .andExpect(jsonPath("$.data.user.email").value("new.client@example.com"));
    }

    @Test
    void loginReturnsValidationErrorForInvalidEmail() throws Exception {
        Map<String, String> body = Map.of(
            "email", "not-an-email",
            "password", "password123"
        );

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("email")));
    }

    @Test
    void loginReturnsUnauthorizedForWrongPassword() throws Exception {
        User user = new User();
        user.setUsername("existing");
        user.setEmail("existing@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.CLIENT);
        userRepository.save(user);

        Map<String, String> body = Map.of(
            "email", "existing@example.com",
            "password", "wrong-password"
        );

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("unauthorized"))
            .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
}
