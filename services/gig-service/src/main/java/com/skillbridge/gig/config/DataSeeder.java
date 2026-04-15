package com.skillbridge.gig.config;

import com.skillbridge.gig.model.Category;
import com.skillbridge.gig.model.Gig;
import com.skillbridge.gig.model.Tag;
import com.skillbridge.gig.repository.CategoryRepository;
import com.skillbridge.gig.repository.GigRepository;
import com.skillbridge.gig.repository.TagRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final GigRepository gigRepository;

    public DataSeeder(CategoryRepository categoryRepository, TagRepository tagRepository, GigRepository gigRepository) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.gigRepository = gigRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            System.out.println("Gig data already seeded, skipping.");
            return;
        }

        System.out.println("Seeding gig data...");

        // Categories
        Category graficki = categoryRepository.save(new Category("Grafički dizajn", "graficki-dizajn"));
        Category programiranje = categoryRepository.save(new Category("Programiranje", "programiranje"));
        Category video = categoryRepository.save(new Category("Video & Animacija", "video-animacija"));
        Category marketing = categoryRepository.save(new Category("Digital Marketing", "digital-marketing"));
        Category uiux = categoryRepository.save(new Category("UI/UX Dizajn", "ui-ux-dizajn"));
        Category copywriting = categoryRepository.save(new Category("Copywriting", "copywriting"));
        Category wordpress = categoryRepository.save(new Category("WordPress", "wordpress"));

        // Tags
        Tag logo = tagRepository.save(new Tag("Logo", "logo"));
        Tag webDizajn = tagRepository.save(new Tag("Web dizajn", "web-dizajn"));
        Tag react = tagRepository.save(new Tag("React developer", "react-developer"));
        Tag seo = tagRepository.save(new Tag("SEO", "seo"));
        Tag videoEditing = tagRepository.save(new Tag("Video editing", "video-editing"));
        Tag branding = tagRepository.save(new Tag("Branding", "branding"));
        Tag nodejs = tagRepository.save(new Tag("Node.js", "node-js"));
        Tag mobileApp = tagRepository.save(new Tag("Mobile app", "mobile-app"));
        Tag figma = tagRepository.save(new Tag("Figma", "figma"));
        Tag youtube = tagRepository.save(new Tag("YouTube", "youtube"));

        // Gigs - Marija (freelancerId=2)
        Gig gig1 = createGig(2, "Marija Kovačević", graficki,
            "Profesionalni dizajn logotipa za vaš brand",
            "Kreirat ću unikatan, profesionalan logotip koji savršeno predstavlja vaš brand. Uključuje:\n\n" +
            "• 3 koncepta logotipa\n• Neograničene revizije na odabranom konceptu\n" +
            "• Sve formate fajlova (AI, EPS, SVG, PNG, JPG)\n• Brand guidelines dokument\n" +
            "• Potpuno vlasništvo nad dizajnom\n\nSvaki dizajn je ručno rađen — bez stock grafika ili šablona.",
            new BigDecimal("150"), 3, 5, List.of(logo, branding));

        Gig gig2 = createGig(2, "Marija Kovačević", graficki,
            "Kompletni branding paket za vaš biznis",
            "Dobit ćete kompletan vizualni identitet koji uključuje logo, boje, tipografiju, vizit karte i social media template. Idealno za nove biznise i rebrand.",
            new BigDecimal("400"), 7, 3, List.of(logo, branding));

        // Gigs - Stefan (freelancerId=3)
        Gig gig3 = createGig(3, "Stefan Milić", programiranje,
            "Full-stack web aplikacija u React & Node.js",
            "Razvit ću vašu web aplikaciju koristeći najmodernije tehnologije:\n\n" +
            "• Frontend: React + TypeScript + Tailwind CSS\n• Backend: Node.js + NestJS + PostgreSQL\n" +
            "• Responsivan dizajn za sve uređaje\n• REST ili GraphQL API\n• Deployment na Vercel/Railway\n\n" +
            "Iskustvo u e-commerce, SaaS, dashboard i CRM aplikacijama.",
            new BigDecimal("500"), 14, 3, List.of(react, nodejs));

        Gig gig4 = createGig(3, "Stefan Milić", programiranje,
            "Custom REST API u Node.js i TypeScript",
            "Dizajnirat ću i implementirati skalabilan REST API za vašu aplikaciju. Uključuje dokumentaciju, testove i deployment.",
            new BigDecimal("300"), 7, 2, List.of(nodejs));

        // Gigs - Ana (freelancerId=4)
        Gig gig5 = createGig(4, "Ana Petrović", video,
            "Montaža i color grading YouTube videa",
            "Profesionalna montaža vaših YouTube videa:\n\n" +
            "• Rezanje i slaganje materijala\n• Color grading i korekcija\n" +
            "• Dodavanje teksta, tranzicija i efekata\n• Zvučni dizajn i muzika\n" +
            "• Thumbnail dizajn (opcionalno)\n\nRadim sa Adobe Premiere Pro i After Effects.",
            new BigDecimal("80"), 5, 2, List.of(videoEditing, youtube));

        Gig gig6 = createGig(4, "Ana Petrović", video,
            "Animirani explainer video za vaš proizvod",
            "60-90 sekundni animirani video koji jasno objašnjava vaš proizvod ili uslugu. Uključuje script, voiceover i animaciju.",
            new BigDecimal("250"), 10, 3, List.of(videoEditing));

        // Gigs - Emir (freelancerId=5)
        Gig gig7 = createGig(5, "Emir Hadžić", marketing,
            "SEO optimizacija vašeg web sajta",
            "Kompletna SEO analiza i optimizacija:\n\n" +
            "• Tehnički SEO audit\n• Keyword istraživanje\n• On-page optimizacija\n" +
            "• Strategija sadržaja\n• Mjesečni izvještaj sa metrikama\n\n" +
            "Povećajte organički promet i rangiranje na Google pretraživaču.",
            new BigDecimal("200"), 7, 2, List.of(seo));

        Gig gig8 = createGig(5, "Emir Hadžić", marketing,
            "Google Ads kampanja setup i optimizacija",
            "Kreirat ću i optimizirati vašu Google Ads kampanju za maksimalan ROI. Uključuje keyword research, ad copy, landing page preporuke.",
            new BigDecimal("180"), 5, 2, List.of(seo));

        // Gigs - Lejla (freelancerId=6)
        Gig gig9 = createGig(6, "Lejla Mujić", uiux,
            "UI/UX dizajn mobilne aplikacije u Figmi",
            "Dizajnirat ću kompletan UI za vašu mobilnu aplikaciju:\n\n" +
            "• User research i personas\n• Wireframes\n• High-fidelity UI dizajn\n" +
            "• Interaktivni prototip\n• Design system sa komponentama\n\n" +
            "Specijalizovana za iOS i Android dizajn.",
            new BigDecimal("350"), 10, 4, List.of(mobileApp, figma));

        Gig gig10 = createGig(6, "Lejla Mujić", uiux,
            "Landing page dizajn u Figmi",
            "Moderan, konverzioni landing page dizajn. Uključuje desktop i mobile verziju, SVG ikonice i responsive grid.",
            new BigDecimal("120"), 3, 3, List.of(webDizajn, figma));

        System.out.println("=== Gig Seed Summary ===");
        System.out.println("  Categories: 7");
        System.out.println("  Tags: 10");
        System.out.println("  Gigs: 10");
        System.out.println("========================");
    }

    private Gig createGig(int freelancerId, String freelancerName, Category category, String title, String description,
                           BigDecimal cost, int deliveryTime, int revisionCount, List<Tag> tags) {
        Gig gig = new Gig();
        gig.setFreelancerId(freelancerId);
        gig.setFreelancerName(freelancerName);
        gig.setCategory(category);
        gig.setTitle(title);
        gig.setDescription(description);
        gig.setCost(cost);
        gig.setDeliveryTime(deliveryTime);
        gig.setRevisionCount(revisionCount);
        gig.getTags().addAll(tags);
        return gigRepository.save(gig);
    }
}
