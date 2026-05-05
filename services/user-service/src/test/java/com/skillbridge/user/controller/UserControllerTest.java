package com.skillbridge.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.user.model.User;
import com.skillbridge.user.model.UserRole;
import com.skillbridge.user.model.Skill;
import com.skillbridge.user.repository.PortfolioItemRepository;
import com.skillbridge.user.repository.SkillRepository;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private SkillRepository skillRepository;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        portfolioItemRepository.deleteAll();
        userRepository.deleteAll();
        skillRepository.deleteAll();

        Skill java = new Skill();
        java.setName("Java");
        java = skillRepository.save(java);

        user = new User();
        user.setUsername("profile.user");
        user.setEmail("profile@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.FREELANCER);
        user.setFirstName("Profile");
        user.setLastName("User");
        user.setCountry("Bosna i Hercegovina");
        user.getSkills().add(java);
        user = userRepository.save(user);
    }

    @Test
    void findAllReturnsPagedUsersWithoutPassword() throws Exception {
        User other = new User();
        other.setUsername("other.client");
        other.setEmail("other@example.com");
        other.setPasswordHash(passwordEncoder.encode("password123"));
        other.setRole(UserRole.CLIENT);
        other.setFirstName("Other");
        other.setCountry("Croatia");
        userRepository.save(other);

        mockMvc.perform(get("/users")
                .param("page", "1")
                .param("limit", "10")
                .param("query", "profile")
                .param("role", "FREELANCER")
                .param("country", "Bosna i Hercegovina")
                .param("skill", "Java")
                .param("sortBy", "username")
                .param("sortDirection", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].email").value("profile@example.com"))
            .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist())
            .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    void findAllRejectsUnsupportedSortField() throws Exception {
        mockMvc.perform(get("/users")
                .param("sortBy", "passwordHash"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("bad_request"))
            .andExpect(jsonPath("$.message").value("Unsupported sort field: passwordHash"));
    }

    @Test
    void getMeReturnsCurrentUserProfile() throws Exception {
        mockMvc.perform(get("/users/me")
                .header("x-user-id", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("profile@example.com"));
    }

    @Test
    void findByIdReturnsPublicProfileWithSkills() throws Exception {
        mockMvc.perform(get("/users/{id}", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.skills[0].name").value("Java"));
    }

    @Test
    void findByIdReturnsNotFoundForInactiveUser() throws Exception {
        user.setIsActive(false);
        userRepository.save(user);

        mockMvc.perform(get("/users/{id}", user.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void updateMeChangesProvidedFields() throws Exception {
        Map<String, String> body = Map.of(
            "firstName", "Updated",
            "bio", "Updated bio"
        );

        mockMvc.perform(patch("/users/me")
                .header("x-user-id", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.firstName").value("Updated"))
            .andExpect(jsonPath("$.data.bio").value("Updated bio"))
            .andExpect(jsonPath("$.data.country").value("Bosna i Hercegovina"));
    }

    @Test
    void patchMeAppliesJsonPatch() throws Exception {
        String patchBody = """
            [
              {"op": "replace", "path": "/bio", "value": "Patched bio"},
              {"op": "replace", "path": "/country", "value": "Germany"}
            ]
            """;

        mockMvc.perform(patch("/users/me")
                .header("x-user-id", user.getId())
                .contentType("application/json-patch+json")
                .content(patchBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.bio").value("Patched bio"))
            .andExpect(jsonPath("$.data.country").value("Germany"));
    }

    @Test
    void patchMeRejectsFieldsOutsideProfilePatchState() throws Exception {
        String patchBody = """
            [
              {"op": "add", "path": "/email", "value": "patched@example.com"}
            ]
            """;

        mockMvc.perform(patch("/users/me")
                .header("x-user-id", user.getId())
                .contentType("application/json-patch+json")
                .content(patchBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("bad_request"))
            .andExpect(jsonPath("$.message").value("Field cannot be patched: email"));
    }

    @Test
    void patchMeValidatesPatchedState() throws Exception {
        String patchBody = """
            [
              {"op": "replace", "path": "/country", "value": "%s"}
            ]
            """.formatted("x".repeat(101));

        mockMvc.perform(patch("/users/me")
                .header("x-user-id", user.getId())
                .contentType("application/json-patch+json")
                .content(patchBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("country")));
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

    @Test
    void deactivateMeMarksUserInactive() throws Exception {
        mockMvc.perform(delete("/users/me")
                .header("x-user-id", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.message").value("User deactivated successfully"));

        mockMvc.perform(get("/users/{id}", user.getId()))
            .andExpect(status().isNotFound());
    }
}
