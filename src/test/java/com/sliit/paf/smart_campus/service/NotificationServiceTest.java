package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.NotificationResponse;
import com.sliit.paf.smart_campus.dto.PageResponse;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.BookingStatus;
import com.sliit.paf.smart_campus.model.Notification;
import com.sliit.paf.smart_campus.model.NotificationType;
import com.sliit.paf.smart_campus.model.Resource;
import com.sliit.paf.smart_campus.model.ResourceStatus;
import com.sliit.paf.smart_campus.model.ResourceType;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketCategory;
import com.sliit.paf.smart_campus.model.TicketPriority;
import com.sliit.paf.smart_campus.model.TicketStatus;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.NotificationRepository;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void notifyBookingCreated_shouldCreateOwnerAndAdminNotifications() {
        User requester = buildUser(1L, "student@example.com", Role.USER);
        Booking booking = Booking.builder()
                .id(10L)
                .resource(Resource.builder()
                        .id(1L)
                        .resourceCode("LAB-001")
                        .name("Advanced Lab")
                        .type(ResourceType.LAB)
                        .capacity(30)
                        .location("Block A")
                        .status(ResourceStatus.ACTIVE)
                        .build())
                .ownerUser(requester)
                .requesterId("student@example.com")
                .purpose("Workshop")
                .expectedAttendees(20)
                .startTime(LocalDateTime.of(2026, 4, 25, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 25, 11, 0))
                .status(BookingStatus.PENDING)
                .build();

        User admin = buildUser(20L, "admin@example.com", Role.ADMIN);

        when(userRepository.findAllByRoleAndActiveTrueOrderByIdAsc(Role.ADMIN)).thenReturn(List.of(admin));
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyBookingCreated(booking);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());

        assertThat(notificationCaptor.getAllValues())
                .extracting(Notification::getRecipientIdentifier)
                .containsExactly("student@example.com", "admin@example.com");
        assertThat(notificationCaptor.getAllValues())
                .extracting(Notification::getRecipientUser)
                .contains(requester, admin);
    }

    @Test
    void notifyTicketAssigned_shouldCreateTechnicianAndReporterNotifications() {
        User reporter = buildUser(30L, "reporter@example.com", Role.USER);
        User technician = buildUser(31L, "tech@example.com", Role.TECHNICIAN);
        Ticket ticket = Ticket.builder()
                .id(11L)
                .title("Broken AC")
                .description("AC is not cooling.")
                .category(TicketCategory.OTHER)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .location("Meeting Room")
                .reportedByUser(reporter)
                .reportedBy("reporter@example.com")
                .assignedTechnicianUser(technician)
                .assignedTechnician("tech@example.com")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyTicketAssigned(ticket);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());

        assertThat(notificationCaptor.getAllValues())
                .extracting(Notification::getRecipientIdentifier)
                .containsExactly("tech@example.com", "reporter@example.com");
        assertThat(notificationCaptor.getAllValues())
                .extracting(Notification::getRecipientUser)
                .contains(technician, reporter);
    }

    @Test
    void notifyTicketStatusChanged_shouldNotifyReporterAndAssignedTechnician() {
        User reporter = buildUser(32L, "reporter@example.com", Role.USER);
        User technician = buildUser(33L, "tech@example.com", Role.TECHNICIAN);
        Ticket ticket = Ticket.builder()
                .id(12L)
                .title("Broken Door")
                .description("Door hinge issue")
                .category(TicketCategory.SAFETY)
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.RESOLVED)
                .location("Block B")
                .reportedByUser(reporter)
                .reportedBy("reporter@example.com")
                .assignedTechnicianUser(technician)
                .assignedTechnician("tech@example.com")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyTicketStatusChanged(ticket);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());

        assertThat(notificationCaptor.getAllValues())
                .extracting(Notification::getRecipientIdentifier)
                .containsExactly("reporter@example.com", "tech@example.com");
    }

    @Test
    void getNotifications_shouldMapRepositoryResultsForNormalUser() {
        User currentUser = buildUser(5L, "member@example.com", Role.USER);
        Notification notification = Notification.builder()
                .id(5L)
                .recipientUser(currentUser)
                .recipientIdentifier("member@example.com")
                .title("Ticket updated")
                .message("Your ticket is now IN_PROGRESS.")
                .type(NotificationType.TICKET_STATUS_UPDATED)
                .relatedEntityType("TICKET")
                .relatedEntityId(22L)
                .isRead(false)
                .createdAt(LocalDateTime.of(2026, 4, 26, 8, 0))
                .build();

        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 1));

        PageResponse<NotificationResponse> responses = notificationService.getNotifications(
                currentUser,
                false,
                "TICKET_STATUS_UPDATED",
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().getFirst().getId()).isEqualTo(5L);
        assertThat(responses.getContent().getFirst().getRecipientEmail()).isEqualTo("member@example.com");
        assertThat(responses.getPage()).isEqualTo(0);
        assertThat(responses.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getNotifications_shouldAllowAdminToViewAllWhenRecipientFilterMissing() {
        User admin = buildUser(6L, "admin@example.com", Role.ADMIN);
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")), 0));

        notificationService.getNotifications(admin, false, null, null, PageRequest.of(0, 20));

        verify(notificationRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void markAsRead_shouldRejectDifferentNormalUser() {
        User currentUser = buildUser(7L, "member@example.com", Role.USER);
        Notification notification = Notification.builder()
                .id(7L)
                .recipientIdentifier("other@example.com")
                .title("Booking approved")
                .message("Approved.")
                .type(NotificationType.BOOKING_APPROVED)
                .isRead(false)
                .build();

        when(notificationRepository.findById(7L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(7L, currentUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only access your own notifications.");
    }

    @Test
    void markAllAsRead_shouldAuditBulkReadAction() {
        User currentUser = buildUser(8L, "member@example.com", Role.USER);
        Notification unreadNotification = Notification.builder()
                .id(8L)
                .recipientUser(currentUser)
                .recipientIdentifier("member@example.com")
                .title("Booking approved")
                .message("Approved.")
                .type(NotificationType.BOOKING_APPROVED)
                .isRead(false)
                .build();

        when(notificationRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(unreadNotification));
        when(notificationRepository.saveAll(org.mockito.ArgumentMatchers.<List<Notification>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int updatedCount = notificationService.markAllAsRead(currentUser, null);

        verify(notificationRepository).saveAll(org.mockito.ArgumentMatchers.<List<Notification>>any());
        verify(auditLogService).recordEvent(eq("NOTIFICATION"), isNull(), eq("NOTIFICATIONS_READ_ALL"), eq(currentUser), eq("member@example.com"), any());
        assertThat(updatedCount).isEqualTo(1);
    }

    private User buildUser(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .displayName("User " + id)
                .provider("LOCAL_DEV")
                .providerId(email)
                .role(role)
                .active(true)
                .build();
    }
}
