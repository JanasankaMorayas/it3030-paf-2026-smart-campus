package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.NotificationResponse;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.BookingStatus;
import com.sliit.paf.smart_campus.model.Notification;
import com.sliit.paf.smart_campus.model.NotificationType;
import com.sliit.paf.smart_campus.model.Resource;
import com.sliit.paf.smart_campus.model.ResourceStatus;
import com.sliit.paf.smart_campus.model.ResourceType;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.NotificationRepository;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void notifyBookingCreated_shouldCreateRequesterAndAdminNotifications() {
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
                .requesterId("student@example.com")
                .purpose("Workshop")
                .expectedAttendees(20)
                .startTime(LocalDateTime.of(2026, 4, 25, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 25, 11, 0))
                .status(BookingStatus.PENDING)
                .build();

        User admin = User.builder()
                .id(20L)
                .email("admin@example.com")
                .displayName("Admin User")
                .provider("LOCAL_DEV")
                .providerId("admin@example.com")
                .role(Role.ADMIN)
                .active(true)
                .build();

        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.empty());
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
                .extracting(Notification::getType)
                .containsOnly(NotificationType.BOOKING_CREATED);
    }

    @Test
    void getNotifications_shouldMapRepositoryResults() {
        Notification notification = Notification.builder()
                .id(5L)
                .recipientIdentifier("member@example.com")
                .title("Ticket updated")
                .message("Your ticket is now IN_PROGRESS.")
                .type(NotificationType.TICKET_STATUS_UPDATED)
                .relatedEntityType("TICKET")
                .relatedEntityId(22L)
                .isRead(false)
                .createdAt(LocalDateTime.of(2026, 4, 26, 8, 0))
                .build();

        when(notificationRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getNotifications("member@example.com", false, "TICKET_STATUS_UPDATED");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getId()).isEqualTo(5L);
        assertThat(responses.getFirst().getType()).isEqualTo(NotificationType.TICKET_STATUS_UPDATED);
    }

    @Test
    void markAsRead_shouldSetReadTimestamp() {
        Notification notification = Notification.builder()
                .id(7L)
                .recipientIdentifier("member@example.com")
                .title("Booking approved")
                .message("Approved.")
                .type(NotificationType.BOOKING_APPROVED)
                .isRead(false)
                .build();

        when(notificationRepository.findByIdAndRecipientIdentifierIgnoreCase(7L, "member@example.com"))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(7L, "member@example.com");

        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        assertThat(response.getRead()).isTrue();
    }
}
