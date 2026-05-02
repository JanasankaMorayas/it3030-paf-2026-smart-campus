package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.model.AuditLog;
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

import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User adminUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        notificationRepository.deleteAll();
        bookingRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = saveUser("admin@example.com", Role.ADMIN);
        memberUser = saveUser("member@example.com", Role.USER);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getAuditLogs_shouldAllowAdminToFilterLogs() throws Exception {
        AuditLog bookingLog = auditLogRepository.save(AuditLog.builder()
                .entityType("BOOKING")
                .entityId(10L)
                .action("BOOKING_CREATED")
                .performedByUser(memberUser)
                .performedByIdentifier("member@example.com")
                .details("Booking created.")
                .build());

        auditLogRepository.save(AuditLog.builder()
                .entityType("TICKET")
                .entityId(20L)
                .action("TICKET_ASSIGNED")
                .performedByUser(adminUser)
                .performedByIdentifier("admin@example.com")
                .details("Assigned technician.")
                .build());

        String from = bookingLog.getCreatedAt().minusMinutes(1).format(DateTimeFormatter.ISO_DATE_TIME);
        String to = bookingLog.getCreatedAt().plusMinutes(1).format(DateTimeFormatter.ISO_DATE_TIME);

        mockMvc.perform(get("/api/audit-logs")
                        .param("entityType", "BOOKING")
                        .param("action", "BOOKING_CREATED")
                        .param("performedBy", "member@example.com")
                        .param("from", from)
                        .param("to", to)
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].entityType").value("BOOKING"))
                .andExpect(jsonPath("$.content[0].performedByEmail").value("member@example.com"))
                .andExpect(jsonPath("$.sort[0]").value("createdAt,desc"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getAuditLogsForEntity_shouldReturnMatchingEntityHistory() throws Exception {
        auditLogRepository.save(AuditLog.builder()
                .entityType("TICKET")
                .entityId(44L)
                .action("TICKET_CREATED")
                .performedByUser(memberUser)
                .performedByIdentifier("member@example.com")
                .details("Created.")
                .build());

        auditLogRepository.save(AuditLog.builder()
                .entityType("TICKET")
                .entityId(99L)
                .action("TICKET_CREATED")
                .performedByUser(memberUser)
                .performedByIdentifier("member@example.com")
                .details("Created.")
                .build());

        mockMvc.perform(get("/api/audit-logs/TICKET/44"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].entityId").value(44L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "USER")
    void getAuditLogs_shouldRejectNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden());
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
}
