package com.skillbridge.gig.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.gig.model.Category;
import com.skillbridge.gig.model.Gig;
import com.skillbridge.gig.model.GigStatus;
import com.skillbridge.gig.repository.CategoryRepository;
import com.skillbridge.gig.repository.GigRepository;
import com.skillbridge.gig.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GigRepository gigRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        gigRepository.deleteAll();
        tagRepository.deleteAll();
        categoryRepository.deleteAll();

        category = categoryRepository.save(new Category("Programiranje", "programiranje"));
    }

    @Test
    void createCreatesGigForValidRequest() throws Exception {
        Map<String, Object> body = Map.of(
            "title", "Full-stack web aplikacija",
            "description", "Razvoj web aplikacije u Spring Boot i React stacku",
            "categoryId", category.getId(),
            "cost", 300,
            "deliveryTime", 7,
            "revisionCount", 2,
            "tags", java.util.List.of("Spring Boot", "React")
        );

        mockMvc.perform(post("/gigs")
                .header("x-user-id", 5)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andExpect(jsonPath("$.data.freelancerId").value(5))
            .andExpect(jsonPath("$.data.title").value("Full-stack web aplikacija"))
            .andExpect(jsonPath("$.data.category.slug").value("programiranje"))
            .andExpect(jsonPath("$.data.tags[0].name").value("Spring Boot"));
    }

    @Test
    void createReturnsValidationErrorForInvalidRequest() throws Exception {
        Map<String, Object> body = Map.of(
            "title", "",
            "categoryId", category.getId(),
            "cost", -10,
            "deliveryTime", 0,
            "revisionCount", -1
        );

        mockMvc.perform(post("/gigs")
                .header("x-user-id", 5)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("title")));
    }

    @Test
    void searchReturnsPagedGigs() throws Exception {
        createGig(5, "Spring Boot API", new BigDecimal("250.00"));
        createGig(6, "Logo dizajn", new BigDecimal("80.00"));

        mockMvc.perform(get("/gigs/search")
                .param("q", "spring")
                .param("page", "1")
                .param("limit", "12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].title").value("Spring Boot API"))
            .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    void searchRejectsInvalidLimit() throws Exception {
        mockMvc.perform(get("/gigs/search")
                .param("page", "1")
                .param("limit", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("limit")));
    }

    @Test
    void searchRejectsInvalidPriceRange() throws Exception {
        mockMvc.perform(get("/gigs/search")
                .param("minPrice", "500")
                .param("maxPrice", "100"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("bad_request"))
            .andExpect(jsonPath("$.message").value("minPrice must be less than or equal to maxPrice"));
    }

    @Test
    void getFeaturedReturnsGigs() throws Exception {
        createGig(5, "Featured Gig 1", new BigDecimal("100.00"));
        createGig(6, "Featured Gig 2", new BigDecimal("200.00"));

        mockMvc.perform(get("/gigs/featured")
                .param("limit", "6"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findByFreelancerReturnsGigs() throws Exception {
        createGig(5, "Freelancer Gig 1", new BigDecimal("150.00"));
        createGig(5, "Freelancer Gig 2", new BigDecimal("250.00"));
        createGig(6, "Other Gig", new BigDecimal("300.00"));

        mockMvc.perform(get("/gigs/freelancer/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void findByIdReturnsGig() throws Exception {
        Gig gig = createGig(5, "Test Gig", new BigDecimal("100.00"));

        mockMvc.perform(get("/gigs/" + gig.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(gig.getId()))
            .andExpect(jsonPath("$.data.title").value("Test Gig"));
    }

    @Test
    void findByIdReturnsNotFoundForInvalidId() throws Exception {
        mockMvc.perform(get("/gigs/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void findByIdReturnsNotFoundForDeletedGig() throws Exception {
        Gig gig = createGig(5, "Deleted Gig", new BigDecimal("100.00"));
        gig.setStatus(GigStatus.DELETED);
        gigRepository.save(gig);

        mockMvc.perform(get("/gigs/" + gig.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void updateUpdatesGigForValidRequest() throws Exception {
        Gig gig = createGig(5, "Original Title", new BigDecimal("100.00"));
        Map<String, Object> body = Map.of("title", "Updated Title");

        mockMvc.perform(patch("/gigs/" + gig.getId())
                .header("x-user-id", 5)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("Updated Title"));
    }

    @Test
    void updateReturnsNotFoundForInvalidId() throws Exception {
        Map<String, Object> body = Map.of("title", "Updated Title");

        mockMvc.perform(patch("/gigs/999")
                .header("x-user-id", 5)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void updateReturnsValidationErrorForInvalidData() throws Exception {
        Gig gig = createGig(5, "Test Gig", new BigDecimal("100.00"));
        Map<String, Object> body = Map.of("cost", -10.0);

        mockMvc.perform(patch("/gigs/" + gig.getId())
                .header("x-user-id", 5)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void updateReturnsNotFoundForDeletedGig() throws Exception {
        Gig gig = createGig(5, "Deleted Gig", new BigDecimal("100.00"));
        gig.setStatus(GigStatus.DELETED);
        gigRepository.save(gig);
        Map<String, Object> body = Map.of("title", "Updated title");

        mockMvc.perform(patch("/gigs/" + gig.getId())
                .header("x-user-id", 5)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void updateRejectsDifferentFreelancer() throws Exception {
        Gig gig = createGig(5, "Spring Boot API", new BigDecimal("250.00"));
        Map<String, Object> body = Map.of("title", "Updated title");

        mockMvc.perform(patch("/gigs/" + gig.getId())
                .header("x-user-id", 99)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("forbidden"))
            .andExpect(jsonPath("$.message").value("You can only edit your own gigs"));
    }

    @Test
    void deleteDeletesGig() throws Exception {
        Gig gig = createGig(5, "Gig to Delete", new BigDecimal("100.00"));

        mockMvc.perform(delete("/gigs/" + gig.getId())
                .header("x-user-id", 5))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteReturnsNotFoundForInvalidId() throws Exception {
        mockMvc.perform(delete("/gigs/999")
                .header("x-user-id", 5))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void deleteReturnsNotFoundForAlreadyDeletedGig() throws Exception {
        Gig gig = createGig(5, "Deleted Gig", new BigDecimal("100.00"));
        gig.setStatus(GigStatus.DELETED);
        gigRepository.save(gig);

        mockMvc.perform(delete("/gigs/" + gig.getId())
                .header("x-user-id", 5))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void deleteRejectsDifferentFreelancer() throws Exception {
        Gig gig = createGig(5, "Gig to Delete", new BigDecimal("100.00"));

        mockMvc.perform(delete("/gigs/" + gig.getId())
                .header("x-user-id", 99))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("forbidden"));
    }

    private Gig createGig(Integer freelancerId, String title, BigDecimal cost) {
        Gig gig = new Gig();
        gig.setFreelancerId(freelancerId);
        gig.setCategory(category);
        gig.setTitle(title);
        gig.setDescription("Opis testnog giga");
        gig.setCost(cost);
        gig.setDeliveryTime(5);
        gig.setRevisionCount(2);
        gig.setStatus(GigStatus.ACTIVE);
        return gigRepository.save(gig);
    }
}
