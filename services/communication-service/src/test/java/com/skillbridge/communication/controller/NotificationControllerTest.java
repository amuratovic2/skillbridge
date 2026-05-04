package com.skillbridge.communication.controller;

import com.skillbridge.communication.model.Notification;
import com.skillbridge.communication.model.NotificationType;
import com.skillbridge.communication.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void findByUserReturnsPagedNotifications() throws Exception {
        createNotification(1, "Stara", true, LocalDateTime.now().minusMinutes(2));
        createNotification(1, "Nova", false, LocalDateTime.now().minusMinutes(1));
        createNotification(2, "Drugi korisnik", false, LocalDateTime.now());

        mockMvc.perform(get("/notifications")
                .header("x-user-id", 1)
                .param("page", "1")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].title").value("Nova"))
            .andExpect(jsonPath("$.data[1].title").value("Stara"))
            .andExpect(jsonPath("$.meta.total").value(2));
    }

    @Test
    void findByUserRejectsInvalidLimit() throws Exception {
        mockMvc.perform(get("/notifications")
                .header("x-user-id", 1)
                .param("limit", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("limit")));
    }

    @Test
    void getUnreadCountCountsOnlyCurrentUser() throws Exception {
        createNotification(1, "Prva", false, LocalDateTime.now().minusMinutes(2));
        createNotification(1, "Druga", true, LocalDateTime.now().minusMinutes(1));
        createNotification(2, "Treci", false, LocalDateTime.now());

        mockMvc.perform(get("/notifications/unread-count")
                .header("x-user-id", 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    void markAsReadUpdatesOwnedNotification() throws Exception {
        Notification notification = createNotification(1, "Neprocitana", false, LocalDateTime.now());

        mockMvc.perform(patch("/notifications/{id}/read", notification.getId())
                .header("x-user-id", 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.isRead").value(true));
    }

    @Test
    void markAsReadRejectsOtherUsersNotification() throws Exception {
        Notification notification = createNotification(2, "Tuda", false, LocalDateTime.now());

        mockMvc.perform(patch("/notifications/{id}/read", notification.getId())
                .header("x-user-id", 1))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void markAllAsReadUpdatesUnreadNotificationsForUser() throws Exception {
        createNotification(1, "Prva", false, LocalDateTime.now().minusMinutes(2));
        createNotification(1, "Druga", false, LocalDateTime.now().minusMinutes(1));
        createNotification(2, "Treci", false, LocalDateTime.now());

        mockMvc.perform(patch("/notifications/read-all")
                .header("x-user-id", 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.updated").value(2));
    }

    private Notification createNotification(Integer userId, String title, boolean read, LocalDateTime createdAt) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(NotificationType.SYSTEM);
        notification.setTitle(title);
        notification.setContent("Test content");
        notification.setIsRead(read);
        notification.setCreatedAt(createdAt);
        return notificationRepository.save(notification);
    }
}
