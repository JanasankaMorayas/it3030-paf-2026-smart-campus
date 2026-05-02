package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.model.Notification;
import com.sliit.paf.smart_campus.model.NotificationType;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.AuditLogRepository;
import com.sliit.paf.smart_campus.repository.BookingRepository;
import com.sliit.paf.smart_campus.repository.NotificationRepository;
import com.sliit.paf.smart_campus.repository.TicketRepository;
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

    private static final String TECHNICIAN_EMAIL = "tech@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User memberUser;
    private User otherUser;
    private User technicianUser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        notificationRepository.deleteAll();
        bookingRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = saveUser("admin@example.com", Role.ADMIN);
        memberUser = saveUser("member@example.com", Role.USER);
        otherUser = saveUser("other@example.com", Role.USER);
        technicianUser = saveUser(TECHNICIAN_EMAIL, Role.TECHNICIAN);
    }

    @Test
    void getNotifications_shouldReturnCurrentUsersNotificationsWithFilters() throws Exception {
        saveNotification(memberUser, "member@example.com", NotificationType.BOOKING_APPROVED, false, 11L);
        saveNotification(memberUser, "member@example.com", NotificationType.TICKET_CREATED, true, 22L);
        saveNotification(otherUser, "other@example.com", NotificationType.BOOKING_APPROVED, false, 33L);

        mockMvc.perform(get("/api/notifications")
                        .param("unreadOnly", "true")
                        .param("type", "BOOKING_APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].relatedEntityId").value(11L))
                .andExpect(jsonPath("$.content[0].recipientEmail").value("member@example.com"))
                .andExpect(jsonPath("$.content[0].type").value("BOOKING_APPROVED"))
                .andExpect(jsonPath("$.sort[0]").value("createdAt,desc"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getNotifications_shouldAllowAdminToViewAllNotifications() throws Exception {
        saveNotification(memberUser, "member@example.com", NotificationType.BOOKING_APPROVED, false, 11L);
        saveNotification(otherUser, "other@example.com", NotificationType.TICKET_CREATED, false, 22L);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getUnreadNotifications_shouldReturnUnreadOnly() throws Exception {
        saveNotification(memberUser, "member@example.com", NotificationType.TICKET_STATUS_UPDATED, false, 44L);
        saveNotification(memberUser, "member@example.com", NotificationType.TICKET_RESOLVED, true, 45L);

        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].relatedEntityId").value(44L));
    }

    @Test
    @WithMockUser(username = TECHNICIAN_EMAIL, roles = "TECHNICIAN")
    void getNotifications_shouldReturnAssignedTechniciansInbox() throws Exception {
        saveNotification(technicianUser, TECHNICIAN_EMAIL, NotificationType.TICKET_ASSIGNED, false, 88L);
        saveNotification(memberUser, "member@example.com", NotificationType.TICKET_ASSIGNED, false, 89L);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].recipientEmail").value(TECHNICIAN_EMAIL))
                .andExpect(jsonPath("$.content[0].type").value("TICKET_ASSIGNED"));
    }

    @Test
    void markAsRead_shouldUpdateNotification() throws Exception {
        Notification notification = saveNotification(memberUser, "member@example.com", NotificationType.BOOKING_CREATED, false, 55L);

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId()))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void markAsRead_shouldAllowAdminToManageOthersNotifications() throws Exception {
        Notification notification = saveNotification(otherUser, "other@example.com", NotificationType.GENERAL, false, null);

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markAllAsRead_shouldReturnSummaryResponse() throws Exception {
        saveNotification(memberUser, "member@example.com", NotificationType.BOOKING_CREATED, false, 66L);
        saveNotification(memberUser, "member@example.com", NotificationType.TICKET_CREATED, false, 67L);

        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notifications marked as read."))
                .andExpect(jsonPath("$.updatedCount").value(2));

        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void deleteNotification_shouldRemoveCurrentUsersNotification() throws Exception {
        Notification notification = saveNotification(memberUser, "member@example.com", NotificationType.GENERAL, false, null);

        mockMvc.perform(delete("/api/notifications/{id}", notification.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getNotifications_shouldReturnForbiddenWhenUserRequestsAnotherInbox() throws Exception {
        saveNotification(otherUser, "other@example.com", NotificationType.GENERAL, false, null);

        mockMvc.perform(get("/api/notifications")
                        .param("recipient", "other@example.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only view your own notifications."));
    }

    private User saveUser(String email, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .displayName(email)
                .provider("LOCAL_DEV")
                .providerId(email)
                .role(role)
                .active(true)
                .build());
    }

    private Notification saveNotification(User recipientUser, String recipientIdentifier, NotificationType type, boolean isRead, Long relatedEntityId) {
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
