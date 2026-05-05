package com.skillbridge.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.user.model.PortfolioItem;
import com.skillbridge.user.model.User;
import com.skillbridge.user.model.UserRole;
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

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioControllerTest {

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
    private User otherUser;
    private PortfolioItem item;

    @BeforeEach
    void setUp() {
        portfolioItemRepository.deleteAll();
        userRepository.deleteAll();
        skillRepository.deleteAll();

        user = createUser("portfolio.user", "portfolio@example.com");
        otherUser = createUser("other.user", "other.portfolio@example.com");

        item = createPortfolioItem(user, "Initial project");
        createPortfolioItem(user, "Second project");
    }

    @Test
    void findByUserIdReturnsPortfolioItemsForActiveUser() throws Exception {
        mockMvc.perform(get("/portfolios/user/{userId}", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findByUserIdReturnsEmptyListForInactiveUser() throws Exception {
        user.setIsActive(false);
        userRepository.save(user);

        mockMvc.perform(get("/portfolios/user/{userId}", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void createAddsPortfolioItemForCurrentUser() throws Exception {
        Map<String, String> body = Map.of(
            "title", "New project",
            "description", "Project description",
            "imageUrl", "https://example.com/image.png"
        );

        mockMvc.perform(post("/portfolios")
                .header("x-user-id", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("New project"))
            .andExpect(jsonPath("$.data.userId").value(user.getId()));
    }

    @Test
    void createValidatesRequiredTitle() throws Exception {
        mockMvc.perform(post("/portfolios")
                .header("x-user-id", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("title")));
    }

    @Test
    void createBatchAddsMultiplePortfolioItems() throws Exception {
        Map<String, Object> body = Map.of("items", List.of(
            Map.of("title", "Batch one", "description", "First"),
            Map.of("title", "Batch two", "description", "Second")
        ));

        mockMvc.perform(post("/portfolios/batch")
                .header("x-user-id", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void createBatchValidatesNestedItems() throws Exception {
        Map<String, Object> body = Map.of("items", List.of(
            Map.of("title", "")
        ));

        mockMvc.perform(post("/portfolios/batch")
                .header("x-user-id", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void updateChangesOwnedPortfolioItem() throws Exception {
        mockMvc.perform(patch("/portfolios/{id}", item.getId())
                .header("x-user-id", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Updated project"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("Updated project"));
    }

    @Test
    void updateRejectsPortfolioItemOwnedByAnotherUser() throws Exception {
        mockMvc.perform(patch("/portfolios/{id}", item.getId())
                .header("x-user-id", otherUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Forbidden update"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void deleteRemovesOwnedPortfolioItem() throws Exception {
        mockMvc.perform(delete("/portfolios/{id}", item.getId())
                .header("x-user-id", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.message").value("Portfolio item deleted successfully"));
    }

    private User createUser(String username, String email) {
        User created = new User();
        created.setUsername(username);
        created.setEmail(email);
        created.setPasswordHash(passwordEncoder.encode("password123"));
        created.setRole(UserRole.FREELANCER);
        return userRepository.save(created);
    }

    private PortfolioItem createPortfolioItem(User owner, String title) {
        PortfolioItem portfolioItem = new PortfolioItem();
        portfolioItem.setUser(owner);
        portfolioItem.setTitle(title);
        portfolioItem.setDescription(title + " description");
        return portfolioItemRepository.save(portfolioItem);
    }
}
