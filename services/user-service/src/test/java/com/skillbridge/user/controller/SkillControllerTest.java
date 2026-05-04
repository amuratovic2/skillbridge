package com.skillbridge.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.user.model.Skill;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private Skill java;
    private Skill spring;

    @BeforeEach
    void setUp() {
        portfolioItemRepository.deleteAll();
        userRepository.deleteAll();
        skillRepository.deleteAll();

        java = createSkill("Java");
        spring = createSkill("Spring");

        user = new User();
        user.setUsername("skill.user");
        user.setEmail("skill@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.FREELANCER);
        user.getSkills().add(java);
        user = userRepository.save(user);
    }

    @Test
    void findAllReturnsSkillsSortedByName() throws Exception {
        mockMvc.perform(get("/skills"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(jsonPath("$.data[0].name").value("Java"));
    }

    @Test
    void createAddsNewSkill() throws Exception {
        mockMvc.perform(post("/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Docker"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Docker"));
    }

    @Test
    void createRejectsDuplicateSkill() throws Exception {
        mockMvc.perform(post("/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Java"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("conflict"));
    }

    @Test
    void createValidatesBlankName() throws Exception {
        mockMvc.perform(post("/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", ""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("name")));
    }

    @Test
    void createBatchAddsMultipleSkills() throws Exception {
        Map<String, Object> body = Map.of("skills", List.of(
            Map.of("name", "React"),
            Map.of("name", "PostgreSQL")
        ));

        mockMvc.perform(post("/skills/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void createBatchRejectsDuplicateInsideBatch() throws Exception {
        Map<String, Object> body = Map.of("skills", List.of(
            Map.of("name", "Docker"),
            Map.of("name", "docker")
        ));

        mockMvc.perform(post("/skills/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void getUserSkillsReturnsEntityGraphLoadedSkills() throws Exception {
        mockMvc.perform(get("/skills/user/{userId}", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].name").value("Java"));
    }

    @Test
    void addSkillToMeAddsNewSkill() throws Exception {
        mockMvc.perform(post("/skills/me/{skillId}", spring.getId())
                .header("x-user-id", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.message").value("Skill added successfully"));
    }

    @Test
    void addSkillToMeRejectsDuplicate() throws Exception {
        mockMvc.perform(post("/skills/me/{skillId}", java.getId())
                .header("x-user-id", user.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("conflict"));
    }

    @Test
    void replaceMySkillsReplacesWholeSkillSetInTransaction() throws Exception {
        mockMvc.perform(put("/skills/me")
                .header("x-user-id", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("skillIds", List.of(spring.getId())))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].name").value("Spring"));
    }

    @Test
    void replaceMySkillsRejectsMissingSkillId() throws Exception {
        mockMvc.perform(put("/skills/me")
                .header("x-user-id", user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("skillIds", List.of(9999)))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void removeSkillFromMeRemovesAssignedSkill() throws Exception {
        mockMvc.perform(delete("/skills/me/{skillId}", java.getId())
                .header("x-user-id", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.message").value("Skill removed successfully"));
    }

    @Test
    void removeSkillFromMeReturnsNotFoundWhenSkillNotAssigned() throws Exception {
        mockMvc.perform(delete("/skills/me/{skillId}", spring.getId())
                .header("x-user-id", user.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    private Skill createSkill(String name) {
        Skill skill = new Skill();
        skill.setName(name);
        return skillRepository.save(skill);
    }
}
