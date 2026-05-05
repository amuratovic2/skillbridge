package com.skillbridge.gig.service;

import com.skillbridge.gig.model.Category;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private GigRepository gigRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        gigRepository.deleteAll();
        tagRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void createBuildsSlugFromTitle() {
        var response = categoryService.create("Graficki dizajn");

        assertThat(response.id()).isNotNull();
        assertThat(response.slug()).isEqualTo("graficki-dizajn");
    }

    @Test
    void createRejectsDuplicateSlug() {
        categoryRepository.save(new Category("Graficki dizajn", "graficki-dizajn"));

        assertThatThrownBy(() -> categoryService.create("Graficki dizajn"))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
    }
}
