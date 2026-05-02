package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.dto.AssignTechnicianRequest;
import com.sliit.paf.smart_campus.dto.CreateTicketRequest;
import com.sliit.paf.smart_campus.dto.UpdateTicketRequest;
import com.sliit.paf.smart_campus.dto.UpdateTicketStatusRequest;
import com.sliit.paf.smart_campus.model.Notification;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketCategory;
import com.sliit.paf.smart_campus.model.TicketPriority;
import com.sliit.paf.smart_campus.model.TicketStatus;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "dev-user@smartcampus.local", roles = "USER")
class TicketControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "dev-admin@smartcampus.local";
    private static final String TECHNICIAN_EMAIL = "dev-tech@smartcampus.local";
    private static final String USER_EMAIL = "dev-user@smartcampus.local";
    private static final String OTHER_EMAIL = "other-user@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User technicianUser;
    private User normalUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        notificationRepository.deleteAll();
        bookingRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = saveUser(ADMIN_EMAIL, Role.ADMIN);
        technicianUser = saveUser(TECHNICIAN_EMAIL, Role.TECHNICIAN);
        normalUser = saveUser(USER_EMAIL, Role.USER);
        otherUser = saveUser(OTHER_EMAIL, Role.USER);
    }

    @Test
    void createTicket_shouldReturnCreatedWhenRequestIsValid() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Water leak in Block A")
                .description("Water leaking near the entrance.")
                .category("PLUMBING")
                .priority("HIGH")
                .location("Block A Entrance")
                .imageUrls(List.of("https://img.example.com/leak-1.jpg"))
                .build();

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Water leak in Block A"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.reportedBy").value(USER_EMAIL))
                .andExpect(jsonPath("$.reporterEmail").value(USER_EMAIL))
                .andExpect(jsonPath("$.imageUrls[0]").value("https://img.example.com/leak-1.jpg"));
    }

    @Test
    void createTicket_shouldReturnForbiddenWhenNormalUserCreatesForAnotherReporter() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Unauthorized ticket")
                .description("Trying to report for another user")
                .category("OTHER")
                .priority("LOW")
                .location("Block B")
                .reportedBy(OTHER_EMAIL)
                .imageUrls(List.of("https://img.example.com/1.jpg"))
                .build();

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only create tickets for your own account."));
    }

    @Test
    void getAllTickets_shouldReturnOnlyCurrentUsersTickets() throws Exception {
        ticketRepository.save(Ticket.builder()
                .title("Network issue")
                .description("Wi-Fi unstable")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.CRITICAL)
                .status(TicketStatus.IN_PROGRESS)
                .location("Library")
                .reportedByUser(normalUser)
                .reportedBy(USER_EMAIL)
                .assignedTechnicianUser(technicianUser)
                .assignedTechnician(TECHNICIAN_EMAIL)
                .build());

        ticketRepository.save(Ticket.builder()
                .title("Dustbin full")
                .description("Need cleaning")
                .category(TicketCategory.CLEANING)
                .priority(TicketPriority.LOW)
                .status(TicketStatus.OPEN)
                .location("Cafeteria")
                .reportedByUser(otherUser)
                .reportedBy(OTHER_EMAIL)
                .build());

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].reportedBy").value(USER_EMAIL))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.sort[0]").value("createdAt,desc"));
    }

    @Test
    @WithMockUser(username = TECHNICIAN_EMAIL, roles = "TECHNICIAN")
    void getAllTickets_shouldReturnAssignedTicketsForTechnician() throws Exception {
        ticketRepository.save(Ticket.builder()
                .title("Assigned to tech")
                .description("Assigned")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .location("Block A")
                .reportedByUser(normalUser)
                .reportedBy(USER_EMAIL)
                .assignedTechnicianUser(technicianUser)
                .assignedTechnician(TECHNICIAN_EMAIL)
                .build());

        ticketRepository.save(Ticket.builder()
                .title("Assigned elsewhere")
                .description("Other")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .location("Block B")
                .reportedByUser(otherUser)
                .reportedBy(OTHER_EMAIL)
                .assignedTechnician("other-tech@example.com")
                .build());

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].technicianEmail").value(TECHNICIAN_EMAIL));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
    void getAllTickets_shouldAllowAdminToFilterOtherReporters() throws Exception {
        ticketRepository.save(Ticket.builder()
                .title("Admin visible 1")
                .description("First")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .location("Block A")
                .reportedByUser(normalUser)
                .reportedBy(USER_EMAIL)
                .assignedTechnicianUser(technicianUser)
                .assignedTechnician(TECHNICIAN_EMAIL)
                .build());

        ticketRepository.save(Ticket.builder()
                .title("Admin visible 2")
                .description("Second")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.IN_PROGRESS)
                .location("Block B")
                .reportedByUser(otherUser)
                .reportedBy(OTHER_EMAIL)
                .build());

        mockMvc.perform(get("/api/tickets")
                        .param("reportedBy", OTHER_EMAIL)
                        .param("status", "IN_PROGRESS")
                        .param("size", "5")
                        .param("sort", "priority,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].reportedBy").value(OTHER_EMAIL))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.sort[0]").value("priority,asc"));
    }

    @Test
    void getTicketById_shouldReturnForbiddenWhenMissingOwnership() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Forbidden ticket")
                .description("Other owner's ticket")
                .category(TicketCategory.SAFETY)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .location("Car Park")
                .reportedByUser(otherUser)
                .reportedBy(OTHER_EMAIL)
                .build());

        mockMvc.perform(get("/api/tickets/{id}", ticket.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only access tickets you reported or that are assigned to you."));
    }

    @Test
    @WithMockUser(username = TECHNICIAN_EMAIL, roles = "TECHNICIAN")
    void getTicketById_shouldAllowAssignedTechnician() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Assigned ticket")
                .description("Assigned")
                .category(TicketCategory.SAFETY)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .location("Server Room")
                .reportedByUser(otherUser)
                .reportedBy(OTHER_EMAIL)
                .assignedTechnicianUser(technicianUser)
                .assignedTechnician(TECHNICIAN_EMAIL)
                .build());

        mockMvc.perform(get("/api/tickets/{id}", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technicianEmail").value(TECHNICIAN_EMAIL));
    }

    @Test
    void getTicketById_shouldReturnNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/tickets/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found with id: 999"));
    }

    @Test
    void createTicket_shouldReturnBadRequestWhenValidationFails() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("")
                .description("")
                .category("INVALID")
                .priority("")
                .location("")
                .imageUrls(List.of("1", "2", "3", "4"))
                .build();

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").value("Title is required."))
                .andExpect(jsonPath("$.validationErrors.category").value("Ticket category must be one of: ELECTRICAL, PLUMBING, NETWORK, CLEANING, SAFETY, OTHER."))
                .andExpect(jsonPath("$.validationErrors.imageUrls").value("A maximum of 3 image URLs is allowed."));
    }

    @Test
    void updateTicket_shouldReturnConflictWhenTicketIsNotEditable() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Closed ticket")
                .description("Already resolved")
                .category(TicketCategory.SAFETY)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.CLOSED)
                .location("Car Park")
                .reportedByUser(normalUser)
                .reportedBy(USER_EMAIL)
                .build());

        UpdateTicketRequest request = UpdateTicketRequest.builder()
                .title("Updated title")
                .description("Updated description")
                .category("SAFETY")
                .priority("CRITICAL")
                .location("Car Park")
                .imageUrls(List.of())
                .build();

        mockMvc.perform(put("/api/tickets/{id}", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only open or in-progress tickets can be updated."));
    }

    @Test
    @WithMockUser(username = TECHNICIAN_EMAIL, roles = "TECHNICIAN")
    void updateTicketStatus_shouldAllowAssignedTechnicianToResolveTicket() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Broken switch")
                .description("Switch needs fixing")
                .category(TicketCategory.ELECTRICAL)
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.IN_PROGRESS)
                .location("Lecture Hall 2")
                .reportedByUser(normalUser)
                .reportedBy(USER_EMAIL)
                .assignedTechnicianUser(technicianUser)
                .assignedTechnician(TECHNICIAN_EMAIL)
                .build());

        UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                .status("RESOLVED")
                .resolutionNotes("Switch replaced and tested.")
                .build();

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.technicianEmail").value(TECHNICIAN_EMAIL))
                .andExpect(jsonPath("$.resolutionNotes").value("Switch replaced and tested."));
    }

    @Test
    void updateTicketStatus_shouldReturnForbiddenWhenReporterTriesToResolve() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Router issue")
                .description("Router unstable")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.IN_PROGRESS)
                .location("IT Lab")
                .reportedByUser(normalUser)
                .reportedBy(USER_EMAIL)
                .assignedTechnicianUser(technicianUser)
                .assignedTechnician(TECHNICIAN_EMAIL)
                .build());

        UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                .status("RESOLVED")
                .resolutionNotes("User should not resolve.")
                .build();

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only admins or the assigned technician can set this ticket status."));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
    void assignTechnician_shouldReturnUpdatedTicket() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Floor cleaning")
                .description("Spill in corridor")
                .category(TicketCategory.CLEANING)
                .priority(TicketPriority.LOW)
                .status(TicketStatus.OPEN)
                .location("Block C")
                .reportedByUser(otherUser)
                .reportedBy(OTHER_EMAIL)
                .build());

        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .assignedTechnician(TECHNICIAN_EMAIL)
                .build();

        mockMvc.perform(patch("/api/tickets/{id}/assign", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTechnician").value(TECHNICIAN_EMAIL))
                .andExpect(jsonPath("$.technicianEmail").value(TECHNICIAN_EMAIL))
                .andExpect(jsonPath("$.assignedTechnicianUserId").value(technicianUser.getId()));
    }

    @Test
    void deleteTicket_shouldCancelOwnedTicket() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Minor safety issue")
                .description("Loose wire")
                .category(TicketCategory.SAFETY)
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .location("Block D")
                .reportedByUser(normalUser)
                .reportedBy(USER_EMAIL)
                .build());

        mockMvc.perform(delete("/api/tickets/{id}", ticket.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tickets/{id}", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
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
