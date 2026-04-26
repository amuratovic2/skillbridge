package com.skillbridge.user.repository;

import com.skillbridge.user.model.PortfolioItem;
import com.skillbridge.user.model.Skill;
import com.skillbridge.user.model.User;
import com.skillbridge.user.model.UserRole;
import com.skillbridge.user.service.UserService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserRepositoryStatisticsTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        portfolioItemRepository.deleteAll();
        userRepository.deleteAll();
        skillRepository.deleteAll();

        Skill java = createSkill("Java");
        Skill spring = createSkill("Spring");

        createUser("first.user", "first@example.com", java, spring);
        createUser("second.user", "second@example.com", java, spring);
    }

    @Test
    void findAllDoesNotTriggerNPlusOneQueriesForUserList() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var response = userService.findAll(1, 10);

        assertThat(response.data()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    private Skill createSkill(String name) {
        Skill skill = new Skill();
        skill.setName(name);
        return skillRepository.save(skill);
    }

    private void createUser(String username, String email, Skill... skills) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.FREELANCER);
        for (Skill skill : skills) {
            user.getSkills().add(skill);
        }
        user = userRepository.save(user);

        PortfolioItem item = new PortfolioItem();
        item.setUser(user);
        item.setTitle(username + " portfolio");
        portfolioItemRepository.save(item);
    }
}
