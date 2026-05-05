package com.skillbridge.gig.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.gig.model.Category;
import com.skillbridge.gig.model.Gig;
import com.skillbridge.gig.model.GigStatus;
import com.skillbridge.gig.repository.CategoryRepository;
import com.skillbridge.gig.repository.GigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private GigRepository gigRepository;

    @BeforeEach
    void setUp() {
        gigRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void findAllReturnsCategories() throws Exception {
        categoryRepository.save(new Category("Programiranje", "programiranje"));
        categoryRepository.save(new Category("Dizajn", "dizajn"));

        mockMvc.perform(get("/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findBySlugReturnsCategory() throws Exception {
        categoryRepository.save(new Category("Programiranje", "programiranje"));

        mockMvc.perform(get("/categories/programiranje"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.slug").value("programiranje"));
    }

    @Test
    void findBySlugReturnsNotFoundForInvalidSlug() throws Exception {
        mockMvc.perform(get("/categories/invalid"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void createCreatesCategory() throws Exception {
        Map<String, Object> body = Map.of("title", "Novi Kategorija");

        mockMvc.perform(post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("Novi Kategorija"));
    }

    @Test
    void createReturnsValidationErrorForInvalidTitle() throws Exception {
        Map<String, Object> body = Map.of("title", "");

        mockMvc.perform(post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void createReturnsConflictForDuplicateSlug() throws Exception {
        categoryRepository.save(new Category("Programiranje", "programiranje"));
        Map<String, Object> body = Map.of("title", "Programiranje");

        mockMvc.perform(post("/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("conflict"));
    }

    @Test
    void updateUpdatesCategory() throws Exception {
        Category category = categoryRepository.save(new Category("Stari Naziv", "stari-naziv"));
        Map<String, Object> body = Map.of("title", "Novi Naziv");

        mockMvc.perform(patch("/categories/" + category.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("Novi Naziv"));
    }

    @Test
    void updateReturnsNotFoundForInvalidId() throws Exception {
        Map<String, Object> body = Map.of("title", "Novi Naziv");

        mockMvc.perform(patch("/categories/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void deleteDeletesCategory() throws Exception {
        Category category = categoryRepository.save(new Category("Kategorija", "kategorija"));

        mockMvc.perform(delete("/categories/" + category.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteReturnsNotFoundForInvalidId() throws Exception {
        mockMvc.perform(delete("/categories/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void deleteReturnsConflictWhenCategoryHasGigs() throws Exception {
        Category category = categoryRepository.save(new Category("Programiranje", "programiranje"));
        createGig(category);

        mockMvc.perform(delete("/categories/" + category.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("conflict"))
            .andExpect(jsonPath("$.message").value("Category has gigs and cannot be deleted"));
    }

    private void createGig(Category category) {
        Gig gig = new Gig();
        gig.setFreelancerId(5);
        gig.setCategory(category);
        gig.setTitle("Spring Boot API");
        gig.setDescription("Opis testnog giga");
        gig.setCost(new BigDecimal("100.00"));
        gig.setDeliveryTime(5);
        gig.setRevisionCount(2);
        gig.setStatus(GigStatus.ACTIVE);
        gigRepository.save(gig);
    }
}
