package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.BookingResponse;
import com.sliit.paf.smart_campus.dto.CreateBookingRequest;
import com.sliit.paf.smart_campus.dto.UpdateBookingStatusRequest;
import com.sliit.paf.smart_campus.exception.BookingConflictException;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.BookingStatus;
import com.sliit.paf.smart_campus.model.Resource;
import com.sliit.paf.smart_campus.model.ResourceStatus;
import com.sliit.paf.smart_campus.model.ResourceType;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.BookingRepository;
import com.sliit.paf.smart_campus.repository.ResourceRepository;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private Authentication adminAuthentication;

    @Mock
    private Authentication userAuthentication;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void createBooking_shouldSavePendingBookingForAuthenticatedOwner() {
        User currentUser = buildUser(100L, "member@example.com", Role.USER);
        Resource resource = buildResource(1L, "LAB-001", "Advanced Lab");
        CreateBookingRequest request = CreateBookingRequest.builder()
                .resourceId(1L)
                .purpose("Database practical")
                .expectedAttendees(30)
                .startTime(LocalDateTime.of(2026, 4, 24, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 24, 11, 0))
                .build();

        Booking savedBooking = Booking.builder()
                .id(10L)
                .resource(resource)
                .ownerUser(currentUser)
                .requesterId("member@example.com")
                .purpose("Database practical")
                .expectedAttendees(30)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.PENDING)
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);
        when(authenticatedUserService.isAdmin(userAuthentication)).thenReturn(false);
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(bookingRepository.existsByResource_IdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(1L), anyCollection(), eq(request.getEndTime()), eq(request.getStartTime())
        )).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(request, userAuthentication);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());

        Booking capturedBooking = bookingCaptor.getValue();
        assertThat(capturedBooking.getOwnerUser()).isEqualTo(currentUser);
        assertThat(capturedBooking.getRequesterId()).isEqualTo("member@example.com");
        assertThat(capturedBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.getOwnerEmail()).isEqualTo("member@example.com");
        verify(auditLogService).recordEvent(eq("BOOKING"), eq(10L), eq("BOOKING_CREATED"), eq(currentUser), eq("member@example.com"), any());
        verify(notificationService).notifyBookingCreated(savedBooking);
    }

    @Test
    void createBooking_shouldRejectOnBehalfCreationForNormalUser() {
        User currentUser = buildUser(101L, "member@example.com", Role.USER);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .resourceId(1L)
                .requesterId("other@example.com")
                .purpose("Networking session")
                .expectedAttendees(25)
                .startTime(LocalDateTime.of(2026, 4, 24, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 24, 11, 0))
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);
        when(authenticatedUserService.isAdmin(userAuthentication)).thenReturn(false);
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(buildResource(1L, "LAB-001", "Advanced Lab")));
        when(bookingRepository.existsByResource_IdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(1L), anyCollection(), eq(request.getEndTime()), eq(request.getStartTime())
        )).thenReturn(false);

        assertThatThrownBy(() -> bookingService.createBooking(request, userAuthentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only create bookings for your own account.");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBooking_shouldThrowConflictWhenOverlapExists() {
        User currentUser = buildUser(102L, "member@example.com", Role.USER);
        Resource resource = buildResource(1L, "LAB-001", "Advanced Lab");
        CreateBookingRequest request = CreateBookingRequest.builder()
                .resourceId(1L)
                .purpose("Networking session")
                .expectedAttendees(25)
                .startTime(LocalDateTime.of(2026, 4, 24, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 24, 11, 0))
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);
        when(authenticatedUserService.isAdmin(userAuthentication)).thenReturn(false);
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(bookingRepository.existsByResource_IdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(1L), anyCollection(), eq(request.getEndTime()), eq(request.getStartTime())
        )).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking(request, userAuthentication))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Booking time overlaps with an existing booking for resource id: 1");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void getBookingById_shouldRejectDifferentNonAdminUser() {
        User currentUser = buildUser(103L, "member@example.com", Role.USER);
        Booking booking = Booking.builder()
                .id(11L)
                .resource(buildResource(1L, "LAB-001", "Advanced Lab"))
                .requesterId("other@example.com")
                .purpose("Existing booking")
                .expectedAttendees(20)
                .startTime(LocalDateTime.of(2026, 4, 24, 12, 0))
                .endTime(LocalDateTime.of(2026, 4, 24, 14, 0))
                .status(BookingStatus.PENDING)
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);
        when(authenticatedUserService.isAdmin(userAuthentication)).thenReturn(false);
        when(bookingRepository.findById(11L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getBookingById(11L, userAuthentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only access your own bookings.");
    }

    @Test
    void updateBookingStatus_shouldApprovePendingBookingForAdmin() {
        User adminUser = buildUser(1L, "dev-admin@smartcampus.local", Role.ADMIN);
        Resource resource = buildResource(1L, "LAB-001", "Advanced Lab");
        Booking booking = Booking.builder()
                .id(15L)
                .resource(resource)
                .requesterId("student-3@example.com")
                .purpose("AI workshop")
                .expectedAttendees(35)
                .startTime(LocalDateTime.of(2026, 4, 25, 10, 0))
                .endTime(LocalDateTime.of(2026, 4, 25, 12, 0))
                .status(BookingStatus.PENDING)
                .build();

        UpdateBookingStatusRequest request = UpdateBookingStatusRequest.builder()
                .status("APPROVED")
                .adminDecisionReason("Approved for scheduled workshop.")
                .build();

        when(authenticatedUserService.getCurrentUser(adminAuthentication)).thenReturn(adminUser);
        when(authenticatedUserService.isAdmin(adminAuthentication)).thenReturn(true);
        when(bookingRepository.findById(15L)).thenReturn(Optional.of(booking));
        when(bookingRepository.existsByResource_IdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                eq(1L), anyCollection(), eq(booking.getEndTime()), eq(booking.getStartTime()), eq(15L)
        )).thenReturn(false);
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = bookingService.updateBookingStatus(15L, request, adminAuthentication);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(booking.getAdminDecisionReason()).isEqualTo("Approved for scheduled workshop.");
        assertThat(response.getStatus()).isEqualTo(BookingStatus.APPROVED);
        verify(auditLogService).recordEvent(eq("BOOKING"), eq(15L), eq("BOOKING_APPROVED"), eq(adminUser), eq("dev-admin@smartcampus.local"), any());
        verify(notificationService).notifyBookingStatusChanged(booking);
    }

    @Test
    void deleteBooking_shouldCancelOwnedApprovedBooking() {
        User currentUser = buildUser(104L, "member@example.com", Role.USER);
        Resource resource = buildResource(1L, "LAB-001", "Advanced Lab");
        Booking booking = Booking.builder()
                .id(18L)
                .resource(resource)
                .ownerUser(currentUser)
                .requesterId("member@example.com")
                .purpose("Cancelled session")
                .expectedAttendees(18)
                .startTime(LocalDateTime.of(2026, 4, 25, 14, 0))
                .endTime(LocalDateTime.of(2026, 4, 25, 16, 0))
                .status(BookingStatus.APPROVED)
                .adminDecisionReason("Previously approved.")
                .build();

        when(authenticatedUserService.getCurrentUser(userAuthentication)).thenReturn(currentUser);
        when(authenticatedUserService.isAdmin(userAuthentication)).thenReturn(false);
        when(bookingRepository.findById(18L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        bookingService.deleteBooking(18L, userAuthentication);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getAdminDecisionReason()).isNull();
        verify(bookingRepository).save(booking);
        verify(auditLogService).recordEvent(eq("BOOKING"), eq(18L), eq("BOOKING_CANCELLED"), eq(currentUser), eq("member@example.com"), any());
        verify(notificationService).notifyBookingStatusChanged(booking);
    }

    private Resource buildResource(Long id, String resourceCode, String name) {
        return Resource.builder()
                .id(id)
                .resourceCode(resourceCode)
                .name(name)
                .type(ResourceType.LAB)
                .capacity(40)
                .location("Block A")
                .status(ResourceStatus.ACTIVE)
                .build();
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
