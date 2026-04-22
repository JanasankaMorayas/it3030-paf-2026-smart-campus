package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.BookingResponse;
import com.sliit.paf.smart_campus.dto.CreateBookingRequest;
import com.sliit.paf.smart_campus.dto.UpdateBookingRequest;
import com.sliit.paf.smart_campus.dto.UpdateBookingStatusRequest;
import com.sliit.paf.smart_campus.exception.BookingConflictException;
import com.sliit.paf.smart_campus.exception.InvalidBookingStateException;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.BookingStatus;
import com.sliit.paf.smart_campus.model.Resource;
import com.sliit.paf.smart_campus.model.ResourceStatus;
import com.sliit.paf.smart_campus.model.ResourceType;
import com.sliit.paf.smart_campus.repository.BookingRepository;
import com.sliit.paf.smart_campus.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void createBooking_shouldSavePendingBooking() {
        Resource resource = buildResource(1L, "LAB-001", "Advanced Lab");
        CreateBookingRequest request = CreateBookingRequest.builder()
                .resourceId(1L)
                .requesterId("student-1")
                .purpose("Database practical")
                .expectedAttendees(30)
                .startTime(LocalDateTime.of(2026, 4, 24, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 24, 11, 0))
                .build();

        Booking savedBooking = Booking.builder()
                .id(10L)
                .resource(resource)
                .requesterId("student-1")
                .purpose("Database practical")
                .expectedAttendees(30)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.PENDING)
                .build();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(bookingRepository.existsByResource_IdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(1L), anyCollection(), eq(request.getEndTime()), eq(request.getStartTime())
        )).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());

        Booking capturedBooking = bookingCaptor.getValue();
        assertThat(capturedBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(capturedBooking.getPurpose()).isEqualTo("Database practical");
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getResourceCode()).isEqualTo("LAB-001");
    }

    @Test
    void createBooking_shouldThrowConflictWhenOverlapExists() {
        Resource resource = buildResource(1L, "LAB-001", "Advanced Lab");
        CreateBookingRequest request = CreateBookingRequest.builder()
                .resourceId(1L)
                .requesterId("student-1")
                .purpose("Networking session")
                .expectedAttendees(25)
                .startTime(LocalDateTime.of(2026, 4, 24, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 24, 11, 0))
                .build();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(bookingRepository.existsByResource_IdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(1L), anyCollection(), eq(request.getEndTime()), eq(request.getStartTime())
        )).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Booking time overlaps with an existing booking for resource id: 1");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void updateBooking_shouldRejectNonPendingBookings() {
        Booking booking = Booking.builder()
                .id(11L)
                .resource(buildResource(1L, "LAB-001", "Advanced Lab"))
                .requesterId("student-2")
                .purpose("Existing booking")
                .expectedAttendees(20)
                .startTime(LocalDateTime.of(2026, 4, 24, 12, 0))
                .endTime(LocalDateTime.of(2026, 4, 24, 14, 0))
                .status(BookingStatus.APPROVED)
                .build();

        UpdateBookingRequest request = UpdateBookingRequest.builder()
                .resourceId(1L)
                .requesterId("student-2")
                .purpose("Edited purpose")
                .expectedAttendees(22)
                .startTime(LocalDateTime.of(2026, 4, 24, 12, 30))
                .endTime(LocalDateTime.of(2026, 4, 24, 14, 30))
                .build();

        when(bookingRepository.findById(11L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.updateBooking(11L, request))
                .isInstanceOf(InvalidBookingStateException.class)
                .hasMessage("Only pending bookings can be updated.");
    }

    @Test
    void updateBookingStatus_shouldApprovePendingBooking() {
        Resource resource = buildResource(1L, "LAB-001", "Advanced Lab");
        Booking booking = Booking.builder()
                .id(15L)
                .resource(resource)
                .requesterId("student-3")
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

        when(bookingRepository.findById(15L)).thenReturn(Optional.of(booking));
        when(bookingRepository.existsByResource_IdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                eq(1L), anyCollection(), eq(booking.getEndTime()), eq(booking.getStartTime()), eq(15L)
        )).thenReturn(false);
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = bookingService.updateBookingStatus(15L, request);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(booking.getAdminDecisionReason()).isEqualTo("Approved for scheduled workshop.");
        assertThat(response.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void deleteBooking_shouldCancelApprovedBooking() {
        Resource resource = buildResource(1L, "LAB-001", "Advanced Lab");
        Booking booking = Booking.builder()
                .id(18L)
                .resource(resource)
                .requesterId("student-4")
                .purpose("Cancelled session")
                .expectedAttendees(18)
                .startTime(LocalDateTime.of(2026, 4, 25, 14, 0))
                .endTime(LocalDateTime.of(2026, 4, 25, 16, 0))
                .status(BookingStatus.APPROVED)
                .adminDecisionReason("Previously approved.")
                .build();

        when(bookingRepository.findById(18L)).thenReturn(Optional.of(booking));

        bookingService.deleteBooking(18L);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getAdminDecisionReason()).isNull();
        verify(bookingRepository).save(booking);
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
}
