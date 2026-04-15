package com.skillbridge.communication.config;

import com.skillbridge.communication.model.*;
import com.skillbridge.communication.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final MessageRepository messageRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    public DataSeeder(MessageRepository messageRepository,
                      ReviewRepository reviewRepository,
                      NotificationRepository notificationRepository) {
        this.messageRepository = messageRepository;
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) {
        if (messageRepository.count() > 0) return;

        seedReviews();
        seedMessages();
        seedNotifications();

        System.out.println("Communication Service: Seeded 2 reviews, 10 messages, 5 notifications");
    }

    private void seedReviews() {
        Review r1 = new Review();
        r1.setOrderId(1);
        r1.setReviewerId(7);
        r1.setRevieweeId(2);
        r1.setRating(5);
        r1.setComment("Odličan rad! Logo je upravo onakav kakav sam zamišljao. Komunikacija na visokom nivou, sve preporučujem.");
        r1.setCreatedAt(LocalDateTime.now().minusDays(5));
        reviewRepository.save(r1);

        Review r2 = new Review();
        r2.setOrderId(4);
        r2.setReviewerId(8);
        r2.setRevieweeId(5);
        r2.setRating(5);
        r2.setComment("SEO optimizacija je donijela vidljive rezultate već nakon mjesec dana. Detaljni izvještaji i profesionalan pristup.");
        r2.setCreatedAt(LocalDateTime.now().minusDays(3));
        reviewRepository.save(r2);
    }

    private void seedMessages() {
        String[][] conv1 = {
            {"7", "2", "Zdravo Marija! Zanima me dizajn logotipa za moj novi startup."},
            {"2", "7", "Zdravo Ahmed! Rado ću pomoći. Možete li mi reći više o vašem brendu - koje su boje, stil i poruka koju želite prenijeti?"},
            {"7", "2", "Radi se o tech startupu, želim moderan minimalistički stil. Boje: plava i bijela."},
            {"2", "7", "Odlično, imam jasnu sliku. Počinjem sa radom, prvi koncepti će biti gotovi za 2 dana!"},
            {"2", "7", "Logo je završen! Pogledajte isporuku i javite mi vaše mišljenje."},
            {"7", "2", "Savršeno, baš ono što sam tražio! Hvala puno!"},
        };

        String[][] conv2 = {
            {"8", "3", "Zdravo Stefane, trebam web aplikaciju za praćenje inventara."},
            {"3", "8", "Zdravo Nina! Zvuči kao zanimljiv projekat. Da li imate wireframe ili specifikaciju?"},
            {"8", "3", "Imam okvirnu specifikaciju, šaljem vam dokument."},
            {"3", "8", "Super, pregledao sam. Krećem sa razvojem, javit ću se za par dana sa prvim rezultatima."},
        };

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < conv1.length; i++) {
            Message m = new Message();
            m.setSenderId(Integer.parseInt(conv1[i][0]));
            m.setReceiverId(Integer.parseInt(conv1[i][1]));
            m.setOrderId(1);
            m.setContent(conv1[i][2]);
            m.setIsRead(true);
            m.setSentAt(now.minusHours(conv1.length - i));
            messageRepository.save(m);
        }

        for (int i = 0; i < conv2.length; i++) {
            Message m = new Message();
            m.setSenderId(Integer.parseInt(conv2[i][0]));
            m.setReceiverId(Integer.parseInt(conv2[i][1]));
            m.setOrderId(2);
            m.setContent(conv2[i][2]);
            m.setIsRead(true);
            m.setSentAt(now.minusHours(conv2.length - i));
            messageRepository.save(m);
        }
    }

    private void seedNotifications() {
        createNotification(2, NotificationType.REVIEW_RECEIVED, "Nova recenzija",
            "Ahmed vam je ostavio 5-zvjezdica recenziju!");
        createNotification(3, NotificationType.ORDER_UPDATE, "Nova narudžba",
            "Primili ste novu narudžbu za web aplikaciju.");
        createNotification(7, NotificationType.ORDER_UPDATE, "Narudžba isporučena",
            "Ana je isporučila vaš video.");
        createNotification(5, NotificationType.REVIEW_RECEIVED, "Nova recenzija",
            "Nina vam je ostavila pozitivnu recenziju!");
        createNotification(6, NotificationType.ORDER_UPDATE, "Nova narudžba",
            "Primili ste narudžbu za UI dizajn mobilne aplikacije.");
    }

    private void createNotification(Integer userId, NotificationType type, String title, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
    }
}
