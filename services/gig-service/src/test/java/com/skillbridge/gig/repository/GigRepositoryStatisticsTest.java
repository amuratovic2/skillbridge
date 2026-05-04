package com.skillbridge.gig.repository;

import com.skillbridge.gig.model.Category;
import com.skillbridge.gig.model.Gig;
import com.skillbridge.gig.model.GigImage;
import com.skillbridge.gig.model.GigStatus;
import com.skillbridge.gig.model.Tag;
import com.skillbridge.gig.service.GigService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GigRepositoryStatisticsTest {

    @Autowired
    private GigService gigService;

    @Autowired
    private GigRepository gigRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Category category;
    private Tag tag;

    @BeforeEach
    void setUp() {
        gigRepository.deleteAll();
        tagRepository.deleteAll();
        categoryRepository.deleteAll();

        category = categoryRepository.save(new Category("Programiranje", "programiranje"));
        tag = tagRepository.save(new Tag("Spring Boot", "spring-boot"));

        for (int i = 1; i <= 6; i++) {
            createGig(i);
        }
    }

    @Test
    void searchDoesNotTriggerNPlusOneQueriesForTagsAndImages() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var response = gigService.search(null, null, null, null, null, null, 1, 10);

        assertThat(response.data()).hasSize(6);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(6);
    }

    private void createGig(int index) {
        Gig gig = new Gig();
        gig.setFreelancerId(10 + index);
        gig.setCategory(category);
        gig.setTitle("Spring Boot gig " + index);
        gig.setDescription("Opis testnog giga");
        gig.setCost(new BigDecimal("100.00").add(BigDecimal.valueOf(index)));
        gig.setDeliveryTime(5);
        gig.setRevisionCount(2);
        gig.setStatus(GigStatus.ACTIVE);
        gig.getTags().add(tag);

        GigImage image = new GigImage();
        image.setImageUrl("https://example.com/image-" + index + ".png");
        image.setSortOrder(index);
        image.setGig(gig);
        gig.getImages().add(image);

        gigRepository.save(gig);
    }
}
