package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.AssignTechnicianRequest;
import com.sliit.paf.smart_campus.dto.CreateTicketRequest;
import com.sliit.paf.smart_campus.dto.TicketResponse;
import com.sliit.paf.smart_campus.dto.UpdateTicketStatusRequest;
import com.sliit.paf.smart_campus.dto.UpdateTicketRequest;
import com.sliit.paf.smart_campus.exception.InvalidTicketStateException;
import com.sliit.paf.smart_campus.exception.TooManyAttachmentsException;
import com.sliit.paf.smart_campus.exception.UserNotFoundException;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketCategory;
import com.sliit.paf.smart_campus.model.TicketPriority;
import com.sliit.paf.smart_campus.model.TicketStatus;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.TicketRepository;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private Authentication adminAuthentication;

    @Mock
    private Authentication technicianAuthentication;

    @Mock
    private Authentication userAuthentication;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void createTicket_shouldSaveOpenTicketForAuthenticatedReporter() {
        User currentUser = buildUser(200L, "member@example.com", Role.USER);
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Projector not working")
                .description("The projector in Hall A is flickering.")
                .category("ELECTRICAL")
                .priority("HIGH")
                .location("Hall A")
                .imageUrls(List.of("https://img.example.com/1.jpg", "https://img.example.com/2.jpg"))
                .build();

        Ticket savedTicket = Ticket.builder()
                .id(1L)
                .title("Projector not working")
                .description("The projector in Hall A is flickering.")
                .category(TicketCategory.ELECTRICAL)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .location("Hall A")
                .reportedByUser(currentUser)
                .reportedBy("member@example.com")
                .imageUrl1("https://img.example.com/1.jpg")
                .imageUrl2("https://img.example.com/2.jpg")
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);

        TicketResponse response = ticketService.createTicket(request, userAuthentication);

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());

        Ticket capturedTicket = ticketCaptor.getValue();
        assertThat(capturedTicket.getReportedByUser()).isEqualTo(currentUser);
        assertThat(capturedTicket.getReportedBy()).isEqualTo("member@example.com");
        assertThat(capturedTicket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.getReporterEmail()).isEqualTo("member@example.com");
        verify(auditLogService).recordEvent(eq("TICKET"), eq(1L), eq("TICKET_CREATED"), eq(currentUser), eq("member@example.com"), any());
        verify(notificationService).notifyTicketCreated(savedTicket);
    }

    @Test
    void createTicket_shouldRejectOnBehalfCreationForNormalUser() {
        User currentUser = buildUser(201L, "member@example.com", Role.USER);
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Overflow attachments")
                .description("Too many images")
                .category("OTHER")
                .priority("LOW")
                .location("Block B")
                .reportedBy("other@example.com")
                .imageUrls(List.of("1"))
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);

        assertThatThrownBy(() -> ticketService.createTicket(request, userAuthentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only create tickets for your own account.");
    }

    @Test
    void createTicket_shouldThrowWhenMoreThanThreeImagesProvided() {
        User currentUser = buildUser(202L, "member@example.com", Role.USER);
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Overflow attachments")
                .description("Too many images")
                .category("OTHER")
                .priority("LOW")
                .location("Block B")
                .imageUrls(List.of("1", "2", "3", "4"))
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);

        assertThatThrownBy(() -> ticketService.createTicket(request, userAuthentication))
                .isInstanceOf(TooManyAttachmentsException.class)
                .hasMessage("A maximum of 3 image URLs is allowed.");
    }

    @Test
    void updateTicket_shouldRejectResolvedTickets() {
        User currentUser = buildUser(203L, "member@example.com", Role.USER);
        Ticket ticket = Ticket.builder()
                .id(2L)
                .title("Resolved ticket")
                .description("Already resolved")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.RESOLVED)
                .location("Lab 1")
                .reportedByUser(currentUser)
                .reportedBy("member@example.com")
                .build();

        UpdateTicketRequest request = UpdateTicketRequest.builder()
                .title("Updated title")
                .description("Updated description")
                .category("NETWORK")
                .priority("HIGH")
                .location("Lab 2")
                .imageUrls(List.of())
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateTicket(2L, request, userAuthentication))
                .isInstanceOf(InvalidTicketStateException.class)
                .hasMessage("Only open or in-progress tickets can be updated.");
    }

    @Test
    void getTicketById_shouldAllowAssignedTechnician() {
        User technicianUser = buildUser(204L, "tech@example.com", Role.TECHNICIAN);
        Ticket ticket = Ticket.builder()
                .id(6L)
                .title("Assigned ticket")
                .description("Assigned")
                .category(TicketCategory.OTHER)
                .priority(TicketPriority.LOW)
                .status(TicketStatus.OPEN)
                .location("Block Z")
                .assignedTechnicianUser(technicianUser)
                .assignedTechnician("tech@example.com")
                .build();

        when(authenticatedUserService.getCurrentUser(technicianAuthentication)).thenReturn(technicianUser);
        when(ticketRepository.findById(6L)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.getTicketById(6L, technicianAuthentication);

        assertThat(response.getTechnicianEmail()).isEqualTo("tech@example.com");
    }

    @Test
    void getTicketById_shouldRejectDifferentNonAdminUser() {
        User currentUser = buildUser(205L, "member@example.com", Role.USER);
        Ticket ticket = Ticket.builder()
                .id(7L)
                .title("Other user ticket")
                .description("Forbidden")
                .category(TicketCategory.OTHER)
                .priority(TicketPriority.LOW)
                .status(TicketStatus.OPEN)
                .location("Block Z")
                .reportedBy("other@example.com")
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.getTicketById(7L, userAuthentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only access tickets you reported or that are assigned to you.");
    }

    @Test
    void updateTicketStatus_shouldAllowAssignedTechnicianToResolve() {
        User technicianUser = buildUser(206L, "tech@example.com", Role.TECHNICIAN);
        Ticket ticket = Ticket.builder()
                .id(3L)
                .title("Need notes")
                .description("Resolution note check")
                .category(TicketCategory.PLUMBING)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.IN_PROGRESS)
                .location("Washroom")
                .assignedTechnicianUser(technicianUser)
                .assignedTechnician("tech@example.com")
                .build();

        UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                .status("RESOLVED")
                .resolutionNotes("Fixed and verified.")
                .build();

        when(authenticatedUserService.getCurrentUser(technicianAuthentication)).thenReturn(technicianUser);
        when(ticketRepository.findById(3L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        TicketResponse response = ticketService.updateTicketStatus(3L, request, technicianAuthentication);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getResolutionNotes()).isEqualTo("Fixed and verified.");
        assertThat(response.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        verify(auditLogService).recordEvent(eq("TICKET"), eq(3L), eq("TICKET_STATUS_CHANGED"), eq(technicianUser), eq("tech@example.com"), any());
        verify(notificationService).notifyTicketStatusChanged(ticket);
    }

    @Test
    void updateTicketStatus_shouldRejectReporterResolvingTicket() {
        User currentUser = buildUser(207L, "member@example.com", Role.USER);
        Ticket ticket = Ticket.builder()
                .id(8L)
                .title("Reporter tries to resolve")
                .description("Forbidden")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.IN_PROGRESS)
                .location("Lab 1")
                .reportedByUser(currentUser)
                .reportedBy("member@example.com")
                .build();

        UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                .status("RESOLVED")
                .resolutionNotes("User should not do this.")
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);
        when(ticketRepository.findById(8L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateTicketStatus(8L, request, userAuthentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only admins or the assigned technician can set this ticket status.");
    }

    @Test
    void assignTechnician_shouldUpdateOpenTicketForAdminWithLinkedTechnicianUser() {
        User adminUser = buildUser(210L, "dev-admin@smartcampus.local", Role.ADMIN);
        User technicianUser = buildUser(208L, "tech@example.com", Role.TECHNICIAN);
        Ticket ticket = Ticket.builder()
                .id(4L)
                .title("Assign me")
                .description("Technician assignment")
                .category(TicketCategory.SAFETY)
                .priority(TicketPriority.CRITICAL)
                .status(TicketStatus.OPEN)
                .location("Gate 2")
                .reportedBy("admin-1")
                .build();

        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .assignedTechnician("tech@example.com")
                .build();

        when(authenticatedUserService.getCurrentUser(adminAuthentication)).thenReturn(adminUser);
        when(authenticatedUserService.isAdmin(adminAuthentication)).thenReturn(true);
        when(ticketRepository.findById(4L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmailIgnoreCase("tech@example.com")).thenReturn(Optional.of(technicianUser));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        TicketResponse response = ticketService.assignTechnician(4L, request, adminAuthentication);

        assertThat(ticket.getAssignedTechnicianUser()).isEqualTo(technicianUser);
        assertThat(ticket.getAssignedTechnician()).isEqualTo("tech@example.com");
        assertThat(response.getTechnicianEmail()).isEqualTo("tech@example.com");
        verify(auditLogService).recordEvent(eq("TICKET"), eq(4L), eq("TICKET_ASSIGNED"), eq(adminUser), eq("dev-admin@smartcampus.local"), any());
        verify(notificationService).notifyTicketAssigned(ticket);
    }

    @Test
    void assignTechnician_shouldRejectMissingTechnicianUser() {
        Ticket ticket = Ticket.builder()
                .id(9L)
                .title("Assign me")
                .description("Technician assignment")
                .category(TicketCategory.SAFETY)
                .priority(TicketPriority.CRITICAL)
                .status(TicketStatus.OPEN)
                .location("Gate 2")
                .reportedBy("admin-1")
                .build();

        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .assignedTechnician("missing-tech@example.com")
                .build();

        when(authenticatedUserService.isAdmin(adminAuthentication)).thenReturn(true);
        when(ticketRepository.findById(9L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmailIgnoreCase("missing-tech@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.assignTechnician(9L, request, adminAuthentication))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with email: missing-tech@example.com");
    }

    @Test
    void deleteTicket_shouldCancelOwnedOpenTicket() {
        User currentUser = buildUser(209L, "member@example.com", Role.USER);
        Ticket ticket = Ticket.builder()
                .id(5L)
                .title("Cancel ticket")
                .description("Cancel flow")
                .category(TicketCategory.CLEANING)
                .priority(TicketPriority.LOW)
                .status(TicketStatus.OPEN)
                .location("Corridor")
                .reportedByUser(currentUser)
                .reportedBy("member@example.com")
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        ticketService.deleteTicket(5L, userAuthentication);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository).save(ticket);
        verify(auditLogService).recordEvent(eq("TICKET"), eq(5L), eq("TICKET_STATUS_CHANGED"), eq(currentUser), eq("member@example.com"), any());
        verify(notificationService).notifyTicketStatusChanged(ticket);
    }

    private User buildUser(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .displayName("Test User")
                .provider("LOCAL_DEV")
                .providerId(email)
                .role(role)
                .active(true)
                .build();
    }
}
