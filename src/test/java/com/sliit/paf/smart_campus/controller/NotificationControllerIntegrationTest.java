package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.model.Notification;
import com.sliit.paf.smart_campus.model.NotificationType;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.NotificationRepository;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "member@example.com", roles = "USER")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User memberUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
        memberUser = userRepository.save(User.builder()
                .email("member@example.com")
                .displayName("Member User")
                .provider("LOCAL_DEV")
                .providerId("member@example.com")
                .role(Role.USER)
                .active(true)
                .build());
    }

    @Test
    void getNotifications_shouldReturnCurrentUsersNotificationsWithFilters() throws Exception {
        saveNotification("member@example.com", NotificationType.BOOKING_APPROVED, false, 11L);
        saveNotification("member@example.com", NotificationType.TICKET_CREATED, true, 22L);
        saveNotification("other@example.com", NotificationType.BOOKING_APPROVED, false, 33L);

        mockMvc.perform(get("/api/notifications")
                        .param("unreadOnly", "true")
                        .param("type", "BOOKING_APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].relatedEntityId").value(11L))
                .andExpect(jsonPath("$[0].type").value("BOOKING_APPROVED"));
    }

    @Test
    void getUnreadNotifications_shouldReturnUnreadOnly() throws Exception {
        saveNotification("member@example.com", NotificationType.TICKET_STATUS_UPDATED, false, 44L);
        saveNotification("member@example.com", NotificationType.TICKET_RESOLVED, true, 45L);

        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].relatedEntityId").value(44L));
    }

    @Test
    void markAsRead_shouldUpdateNotification() throws Exception {
        Notification notification = saveNotification("member@example.com", NotificationType.BOOKING_CREATED, false, 55L);

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId()))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markAllAsRead_shouldReturnNoContent() throws Exception {
        saveNotification("member@example.com", NotificationType.BOOKING_CREATED, false, 66L);
        saveNotification("member@example.com", NotificationType.TICKET_CREATED, false, 67L);

        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteNotification_shouldRemoveCurrentUsersNotification() throws Exception {
        Notification notification = saveNotification("member@example.com", NotificationType.GENERAL, false, null);

        mockMvc.perform(delete("/api/notifications/{id}", notification.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private Notification saveNotification(String recipientIdentifier, NotificationType type, boolean isRead, Long relatedEntityId) {
        User recipientUser = "member@example.com".equalsIgnoreCase(recipientIdentifier) ? memberUser : null;
        return notificationRepository.save(Notification.builder()
                .recipientUser(recipientUser)
                .recipientIdentifier(recipientIdentifier)
                .title("Notification")
                .message("Test notification")
                .type(type)
                .relatedEntityType(relatedEntityId == null ? "GENERAL" : "BOOKING")
                .relatedEntityId(relatedEntityId)
                .isRead(isRead)
                .readAt(isRead ? LocalDateTime.of(2026, 4, 27, 10, 0) : null)
                .build());
    }
}
