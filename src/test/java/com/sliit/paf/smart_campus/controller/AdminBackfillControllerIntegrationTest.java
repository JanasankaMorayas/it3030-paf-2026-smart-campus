package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.model.AuditLog;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.BookingStatus;
import com.sliit.paf.smart_campus.model.Resource;
import com.sliit.paf.smart_campus.model.ResourceStatus;
import com.sliit.paf.smart_campus.model.ResourceType;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketCategory;
import com.sliit.paf.smart_campus.model.TicketPriority;
import com.sliit.paf.smart_campus.model.TicketStatus;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.AuditLogRepository;
import com.sliit.paf.smart_campus.repository.BookingRepository;
import com.sliit.paf.smart_campus.repository.ResourceRepository;
import com.sliit.paf.smart_campus.repository.TicketRepository;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminBackfillControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User ownerUser;
    private User reporterUser;
    private User technicianUser;
    private User nonTechnicianUser;
    private Booking legacyBooking;
    private Ticket legacyReporterTicket;
    private Ticket legacyTechnicianTicket;
    private Ticket skippedTechnicianTicket;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        bookingRepository.deleteAll();
        ticketRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = saveUser("admin@example.com", Role.ADMIN);
        ownerUser = saveUser("owner@example.com", Role.USER);
        reporterUser = saveUser("reporter@example.com", Role.USER);
        technicianUser = saveUser("tech@example.com", Role.TECHNICIAN);
        nonTechnicianUser = saveUser("not-tech@example.com", Role.USER);

        Resource resource = resourceRepository.save(Resource.builder()
                .resourceCode("LAB-400")
                .name("Backfill Lab")
                .description("Backfill test resource")
                .type(ResourceType.LAB)
                .capacity(40)
                .location("Block Z")
                .status(ResourceStatus.ACTIVE)
                .build());

        legacyBooking = bookingRepository.save(Booking.builder()
                .resource(resource)
                .requesterId("owner@example.com")
                .purpose("Legacy booking")
                .expectedAttendees(20)
                .startTime(java.time.LocalDateTime.of(2026, 4, 29, 9, 0))
                .endTime(java.time.LocalDateTime.of(2026, 4, 29, 10, 0))
                .status(BookingStatus.PENDING)
                .build());

        legacyReporterTicket = ticketRepository.save(Ticket.builder()
                .title("Legacy reporter")
                .description("Legacy reporter record")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .location("Block A")
                .reportedBy("reporter@example.com")
                .build());

        legacyTechnicianTicket = ticketRepository.save(Ticket.builder()
                .title("Legacy technician")
                .description("Legacy tech assignment")
                .category(TicketCategory.ELECTRICAL)
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .location("Block B")
                .reportedBy("reporter@example.com")
                .assignedTechnician("tech@example.com")
                .build());

        skippedTechnicianTicket = ticketRepository.save(Ticket.builder()
                .title("Skip technician")
                .description("Non technician user")
                .category(TicketCategory.OTHER)
                .priority(TicketPriority.LOW)
                .status(TicketStatus.OPEN)
                .location("Block C")
                .reportedBy("reporter@example.com")
                .assignedTechnician("not-tech@example.com")
                .build());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void backfillUserLinks_shouldLinkMatchingLegacyRecords() throws Exception {
        mockMvc.perform(post("/api/admin/backfill/user-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsScanned").value(6))
                .andExpect(jsonPath("$.recordsLinked").value(5))
                .andExpect(jsonPath("$.recordsSkipped").value(1))
                .andExpect(jsonPath("$.bookingsLinked").value(1))
                .andExpect(jsonPath("$.ticketReportersLinked").value(3))
                .andExpect(jsonPath("$.ticketTechniciansLinked").value(1));

        Booking reloadedBooking = bookingRepository.findById(legacyBooking.getId()).orElseThrow();
        Ticket reloadedReporterTicket = ticketRepository.findById(legacyReporterTicket.getId()).orElseThrow();
        Ticket reloadedTechnicianTicket = ticketRepository.findById(legacyTechnicianTicket.getId()).orElseThrow();
        Ticket reloadedSkippedTechnicianTicket = ticketRepository.findById(skippedTechnicianTicket.getId()).orElseThrow();

        assertThat(reloadedBooking.getOwnerUser()).isNotNull();
        assertThat(reloadedBooking.getOwnerUser().getEmail()).isEqualTo(ownerUser.getEmail());
        assertThat(reloadedReporterTicket.getReportedByUser()).isNotNull();
        assertThat(reloadedReporterTicket.getReportedByUser().getEmail()).isEqualTo(reporterUser.getEmail());
        assertThat(reloadedTechnicianTicket.getAssignedTechnicianUser()).isNotNull();
        assertThat(reloadedTechnicianTicket.getAssignedTechnicianUser().getEmail()).isEqualTo(technicianUser.getEmail());
        assertThat(reloadedSkippedTechnicianTicket.getAssignedTechnicianUser()).isNull();

        assertThat(auditLogRepository.findAll())
                .extracting(AuditLog::getAction)
                .contains("USER_LINK_BACKFILL");
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void backfillUserLinks_shouldRejectNonAdminUsers() throws Exception {
        mockMvc.perform(post("/api/admin/backfill/user-links"))
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
