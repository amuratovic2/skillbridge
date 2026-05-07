package com.skillbridge.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.user.model.User;
import com.skillbridge.user.model.UserRole;
import com.skillbridge.user.repository.RefreshTokenRepository;
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
import org.springframework.test.web.servlet.MvcResult;

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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
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

    @Test
    void refreshRotatesRefreshToken() throws Exception {
        JsonNode registerResponse = registerUser("refresh.user", "refresh@example.com");
        String refreshToken = registerResponse.at("/data/refreshToken").asText();

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken", notNullValue()))
            .andExpect(jsonPath("$.data.refreshToken", notNullValue()));
    }

    @Test
    void logoutInvalidatesRefreshToken() throws Exception {
        JsonNode registerResponse = registerUser("logout.user", "logout@example.com");
        String refreshToken = registerResponse.at("/data/refreshToken").asText();

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.message").value("Logged out successfully"));

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void validateReturnsTokenClaimsForValidAccessToken() throws Exception {
        JsonNode registerResponse = registerUser("validate.user", "validate@example.com");
        String accessToken = registerResponse.at("/data/accessToken").asText();

        mockMvc.perform(post("/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", accessToken))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("validate@example.com"))
            .andExpect(jsonPath("$.data.role").value("CLIENT"));
    }

    @Test
    void validateRejectsRefreshToken() throws Exception {
        JsonNode registerResponse = registerUser("refresh-as-access.user", "refresh-as-access@example.com");
        String refreshToken = registerResponse.at("/data/refreshToken").asText();

        mockMvc.perform(post("/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", refreshToken))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void validateRejectsInvalidToken() throws Exception {
        mockMvc.perform(post("/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", "not-a-token"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    private JsonNode registerUser(String username, String email) throws Exception {
        Map<String, String> body = Map.of(
            "username", username,
            "email", email,
            "password", "password123",
            "role", "CLIENT",
            "firstName", "Test",
            "lastName", "User"
        );

        MvcResult result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
