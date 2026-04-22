package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.BookingResponse;
import com.sliit.paf.smart_campus.dto.CreateBookingRequest;
import com.sliit.paf.smart_campus.dto.UpdateBookingRequest;
import com.sliit.paf.smart_campus.dto.UpdateBookingStatusRequest;
import com.sliit.paf.smart_campus.exception.BookingConflictException;
import com.sliit.paf.smart_campus.exception.BookingNotFoundException;
import com.sliit.paf.smart_campus.exception.InvalidBookingStateException;
import com.sliit.paf.smart_campus.exception.ResourceNotFoundException;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.BookingStatus;
import com.sliit.paf.smart_campus.model.Resource;
import com.sliit.paf.smart_campus.repository.BookingRepository;
import com.sliit.paf.smart_campus.repository.BookingSpecifications;
import com.sliit.paf.smart_campus.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BookingService {

    private static final Set<BookingStatus> BLOCKING_STATUSES = EnumSet.of(BookingStatus.PENDING, BookingStatus.APPROVED);

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;

    public BookingService(BookingRepository bookingRepository, ResourceRepository resourceRepository) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
    }

    public List<BookingResponse> getAllBookings(Long resourceId, String requesterId, String status) {
        return bookingRepository.findAll(
                        BookingSpecifications.hasResourceId(resourceId)
                                .and(BookingSpecifications.hasRequesterId(requesterId))
                                .and(BookingSpecifications.hasStatus(parseStatus(status)))
                ).stream()
                .map(BookingResponse::from)
                .toList();
    }

    public BookingResponse getBookingById(Long id) {
        return BookingResponse.from(findBookingById(id));
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        Resource resource = findResourceById(request.getResourceId());
        validateBookingConflict(resource.getId(), request.getStartTime(), request.getEndTime(), null);

        Booking booking = Booking.builder()
                .resource(resource)
                .requesterId(normalizeText(request.getRequesterId()))
                .purpose(normalizeText(request.getPurpose()))
                .expectedAttendees(request.getExpectedAttendees())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.PENDING)
                .build();

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse updateBooking(Long id, UpdateBookingRequest request) {
        Booking booking = findBookingById(id);
        validateBookingCanBeEdited(booking);

        Resource resource = findResourceById(request.getResourceId());
        validateBookingConflict(resource.getId(), request.getStartTime(), request.getEndTime(), booking.getId());

        booking.setResource(resource);
        booking.setRequesterId(normalizeText(request.getRequesterId()));
        booking.setPurpose(normalizeText(request.getPurpose()));
        booking.setExpectedAttendees(request.getExpectedAttendees());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse updateBookingStatus(Long id, UpdateBookingStatusRequest request) {
        Booking booking = findBookingById(id);
        BookingStatus targetStatus = BookingStatus.from(request.getStatus());

        validateStatusTransition(booking.getStatus(), targetStatus);

        if (targetStatus == BookingStatus.APPROVED) {
            validateBookingConflict(booking.getResource().getId(), booking.getStartTime(), booking.getEndTime(), booking.getId());
        }

        if (targetStatus == BookingStatus.REJECTED && !StringUtils.hasText(request.getAdminDecisionReason())) {
            throw new IllegalArgumentException("Admin decision reason is required when rejecting a booking.");
        }

        booking.setStatus(targetStatus);
        booking.setAdminDecisionReason(normalizeNullableText(request.getAdminDecisionReason()));

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = findBookingById(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        validateStatusTransition(booking.getStatus(), BookingStatus.CANCELLED);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setAdminDecisionReason(null);
        bookingRepository.save(booking);
    }

    private Booking findBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
    }

    private Resource findResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    private void validateBookingConflict(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, Long currentBookingId) {
        boolean hasConflict = currentBookingId == null
                ? bookingRepository.existsByResource_IdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        resourceId,
                        BLOCKING_STATUSES,
                        endTime,
                        startTime
                )
                : bookingRepository.existsByResource_IdAndStatusInAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        resourceId,
                        BLOCKING_STATUSES,
                        endTime,
                        startTime,
                        currentBookingId
                );

        if (hasConflict) {
            throw new BookingConflictException("Booking time overlaps with an existing booking for resource id: " + resourceId);
        }
    }

    private void validateBookingCanBeEdited(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStateException("Only pending bookings can be updated.");
        }
    }

    private void validateStatusTransition(BookingStatus currentStatus, BookingStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        boolean validTransition = switch (currentStatus) {
            case PENDING -> targetStatus == BookingStatus.APPROVED
                    || targetStatus == BookingStatus.REJECTED
                    || targetStatus == BookingStatus.CANCELLED;
            case APPROVED -> targetStatus == BookingStatus.CANCELLED;
            case REJECTED, CANCELLED -> false;
        };

        if (!validTransition) {
            throw new InvalidBookingStateException(
                    "Invalid booking status transition from " + currentStatus + " to " + targetStatus + "."
            );
        }
    }

    private BookingStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return BookingStatus.from(status);
    }

    private String normalizeText(String value) {
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
