package com.skillbridge.gig.service;

import com.skillbridge.gig.dto.CreateGigRequest;
import com.skillbridge.gig.dto.GigResponse;
import com.skillbridge.gig.dto.UpdateGigRequest;
import com.skillbridge.gig.model.Category;
import com.skillbridge.gig.model.Gig;
import com.skillbridge.gig.model.GigStatus;
import com.skillbridge.gig.repository.CategoryRepository;
import com.skillbridge.gig.repository.GigRepository;
import com.skillbridge.gig.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class GigServiceTest {

    @Autowired
    private GigService gigService;

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
    void createPersistsGigAndTags() {
        CreateGigRequest request = new CreateGigRequest();
        request.setTitle("REST API implementacija");
        request.setDescription("Implementacija REST API-ja u Spring Bootu");
        request.setCategoryId(category.getId());
        request.setCost(new BigDecimal("220.00"));
        request.setDeliveryTime(6);
        request.setRevisionCount(2);
        request.setTags(List.of("Spring Boot", "PostgreSQL"));

        GigResponse response = gigService.create(7, request);

        assertThat(response.id()).isNotNull();
        assertThat(response.freelancerId()).isEqualTo(7);
        assertThat(response.category().slug()).isEqualTo("programiranje");
        assertThat(response.tags()).extracting("slug").contains("spring-boot", "postgresql");
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        Gig gig = createGig(7, "Originalni naslov", new BigDecimal("200.00"));

        UpdateGigRequest request = new UpdateGigRequest();
        request.setTitle("Novi naslov");
        request.setRevisionCount(4);

        GigResponse response = gigService.update(gig.getId(), 7, request);

        assertThat(response.title()).isEqualTo("Novi naslov");
        assertThat(response.revisionCount()).isEqualTo(4);
        assertThat(response.cost()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void findByIdThrowsNotFoundForMissingGig() {
        assertThatThrownBy(() -> gigService.findById(9999))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateRejectsDifferentFreelancer() {
        Gig gig = createGig(7, "Originalni naslov", new BigDecimal("200.00"));

        UpdateGigRequest request = new UpdateGigRequest();
        request.setTitle("Nedozvoljena izmjena");

        assertThatThrownBy(() -> gigService.update(gig.getId(), 9, request))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteMarksGigAsDeleted() {
        Gig gig = createGig(7, "Originalni naslov", new BigDecimal("200.00"));

        var response = gigService.delete(gig.getId(), 7);

        assertThat(response).containsEntry("message", "Gig deleted successfully");
        assertThat(gigRepository.findById(gig.getId()).orElseThrow().getStatus()).isEqualTo(GigStatus.DELETED);
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
