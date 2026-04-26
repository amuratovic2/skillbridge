package com.skillbridge.user.config;

import com.skillbridge.user.model.*;
import com.skillbridge.user.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, SkillRepository skillRepository,
                      PortfolioItemRepository portfolioItemRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.portfolioItemRepository = portfolioItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        String hash = passwordEncoder.encode("password123");

        String[] skillNames = {"Logo", "Branding", "Vektorski dizajn", "Komunikacija", "Deadline poštovanje",
            "React", "Node.js", "TypeScript", "PostgreSQL", "UI/UX dizajn", "SEO", "Copywriting",
            "Video editing", "Adobe Premiere", "Adobe Illustrator", "Figma", "WordPress", "Python", "Marketing", "Fotografija"};
        for (String name : skillNames) {
            Skill s = new Skill();
            s.setName(name);
            skillRepository.save(s);
        }
        List<Skill> skills = skillRepository.findAll();

        User admin = createUser(hash, "admin", "admin@skillbridge.ba", UserRole.ADMIN, "Admin", "SkillBridge", "Administrator platforme.", "Bosna i Hercegovina");
        User marija = createUser(hash, "marija.kovacevic", "marija@example.com", UserRole.FREELANCER, "Marija", "Kovačević",
            "Kreiram jedinstvene, moderne logotipe koji odražavaju identitet vašeg branda. Svaki dizajn je ručno rađen u Adobe Illustratoru.", "Srbija");
        User stefan = createUser(hash, "stefan.milic", "stefan@example.com", UserRole.FREELANCER, "Stefan", "Milić",
            "Full-stack developer sa 5+ godina iskustva u React i Node.js ekosistemu.", "Srbija");
        User ana = createUser(hash, "ana.petrovic", "ana@example.com", UserRole.FREELANCER, "Ana", "Petrović",
            "Profesionalna video editorka specijalizovana za YouTube content.", "Hrvatska");
        User emir = createUser(hash, "emir.hadzic", "emir@example.com", UserRole.FREELANCER, "Emir", "Hadžić",
            "SEO stručnjak i digital marketing konzultant.", "Bosna i Hercegovina");
        User lejla = createUser(hash, "lejla.mujic", "lejla@example.com", UserRole.FREELANCER, "Lejla", "Mujić",
            "UI/UX dizajnerica sa fokusom na korisničko iskustvo.", "Bosna i Hercegovina");
        createUser(hash, "ahmed.basic", "ahmed@example.com", UserRole.CLIENT, "Ahmed", "Bašić", null, "Bosna i Hercegovina");
        createUser(hash, "nina.jovanovic", "nina@example.com", UserRole.CLIENT, "Nina", "Jovanović", null, "Srbija");

        assignSkills(marija, skills, 0, 1, 2, 3, 4, 14);
        assignSkills(stefan, skills, 5, 6, 7, 8, 15);
        assignSkills(ana, skills, 12, 13, 3, 4);
        assignSkills(emir, skills, 10, 11, 18);
        assignSkills(lejla, skills, 9, 15, 3, 5);

        createPortfolio(marija, "Logo za tech startup", "Minimalistički logo za SaaS kompaniju");
        createPortfolio(marija, "Branding paket za restoran", "Kompletni vizualni identitet");
        createPortfolio(marija, "Logo redizajn za NGO", "Modernizacija postojećeg logotipa");
        createPortfolio(stefan, "E-commerce platforma", "Full-stack web shop sa React i Node.js");
        createPortfolio(stefan, "Dashboard za analitiku", "Real-time dashboard za praćenje metrika");
        createPortfolio(ana, "YouTube kanal montaža", "Regularni video sadržaj za gaming kanal");
        createPortfolio(ana, "Promo video za startup", "60-sekundni animirani explainer video");
        createPortfolio(lejla, "Mobile app dizajn", "UI dizajn za fitness aplikaciju");
        createPortfolio(lejla, "SaaS dashboard", "Kompletan UI kit za project management tool");

        System.out.println("User Service: Seeded 8 users, " + skills.size() + " skills, 9 portfolio items");
    }

    private User createUser(String hash, String username, String email, UserRole role,
                            String firstName, String lastName, String bio, String country) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setRole(role);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setBio(bio);
        u.setCountry(country);
        return userRepository.save(u);
    }

    private void assignSkills(User user, List<Skill> allSkills, int... indices) {
        for (int i : indices) {
            user.getSkills().add(allSkills.get(i));
        }
        userRepository.save(user);
    }

    private void createPortfolio(User user, String title, String description) {
        PortfolioItem item = new PortfolioItem();
        item.setUser(user);
        item.setTitle(title);
        item.setDescription(description);
        portfolioItemRepository.save(item);
    }
}
