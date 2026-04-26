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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user = new User();
        user.setUsername("profile.user");
        user.setEmail("profile@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.FREELANCER);
        user.setFirstName("Profile");
        user.setLastName("User");
        user = userRepository.save(user);
    }

    @Test
    void findAllReturnsPagedUsersWithoutPassword() throws Exception {
        mockMvc.perform(get("/users")
                .param("page", "1")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].email").value("profile@example.com"))
            .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist())
            .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    void updateMeValidatesFieldLengths() throws Exception {
        Map<String, String> body = Map.of("country", "x".repeat(101));

        mockMvc.perform(patch("/users/me")
                .header("x-user-id", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("country")));
    }
}
