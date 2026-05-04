package com.skillbridge.gig.controller;

import com.skillbridge.gig.model.Tag;
import com.skillbridge.gig.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        tagRepository.deleteAll();
    }

    @Test
    void findAllReturnsTags() throws Exception {
        tagRepository.save(new Tag("Spring Boot", "spring-boot"));
        tagRepository.save(new Tag("React", "react"));

        mockMvc.perform(get("/tags"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findPopularReturnsTags() throws Exception {
        tagRepository.save(new Tag("Spring Boot", "spring-boot"));
        tagRepository.save(new Tag("React", "react"));

        mockMvc.perform(get("/tags/popular")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findPopularRejectsInvalidLimit() throws Exception {
        mockMvc.perform(get("/tags/popular")
                .param("limit", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"));
    }
}