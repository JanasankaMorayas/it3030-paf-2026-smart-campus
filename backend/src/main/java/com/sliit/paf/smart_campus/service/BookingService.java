package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.BookingResponse;
import com.sliit.paf.smart_campus.dto.CreateBookingRequest;
import com.sliit.paf.smart_campus.dto.PageResponse;
import com.sliit.paf.smart_campus.dto.UpdateBookingRequest;
import com.sliit.paf.smart_campus.dto.UpdateBookingStatusRequest;
import com.sliit.paf.smart_campus.exception.BookingConflictException;
import com.sliit.paf.smart_campus.exception.BookingNotFoundException;
import com.sliit.paf.smart_campus.exception.InvalidBookingStateException;
import com.sliit.paf.smart_campus.exception.ResourceNotFoundException;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.BookingStatus;
import com.sliit.paf.smart_campus.model.Resource;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.BookingRepository;
import com.sliit.paf.smart_campus.repository.BookingSpecifications;
import com.sliit.paf.smart_campus.repository.ResourceRepository;
import com.sliit.paf.smart_campus.repository.UserRepository;
import com.sliit.paf.smart_campus.util.PageableUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BookingService {

    private static final Set<BookingStatus> BLOCKING_STATUSES = EnumSet.of(BookingStatus.PENDING, BookingStatus.APPROVED);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "startTime", "endTime", "status", "expectedAttendees", "createdAt", "updatedAt"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.asc("startTime"), Sort.Order.desc("createdAt"));

    private final AuthenticatedUserService authenticatedUserService;
    private final AuditLogService auditLogService;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public BookingService(
            AuditLogService auditLogService,
            BookingRepository bookingRepository,
            ResourceRepository resourceRepository,
            NotificationService notificationService,
            AuthenticatedUserService authenticatedUserService,
            UserRepository userRepository
    ) {
        this.auditLogService = auditLogService;
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.notificationService = notificationService;
        this.authenticatedUserService = authenticatedUserService;
        this.userRepository = userRepository;
    }

    public PageResponse<BookingResponse> getAllBookings(
            Long resourceId,
            String requesterId,
            String status,
            Authentication authentication,
            Pageable pageable
    ) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        boolean admin = authenticatedUserService.isAdmin(authentication);
        String visibleRequesterId = resolveVisibleRequesterFilter(currentUser, admin, requesterId);
        Pageable sanitizedPageable = PageableUtils.sanitize(pageable, DEFAULT_SORT, ALLOWED_SORT_FIELDS);

        Page<BookingResponse> bookingPage = bookingRepository.findAll(
                        BookingSpecifications.hasResourceId(resourceId)
                                .and(BookingSpecifications.hasOwnerIdentifier(visibleRequesterId))
                                .and(BookingSpecifications.hasStatus(parseStatus(status))),
                        sanitizedPageable
                ).map(BookingResponse::from);

        return PageResponse.from(bookingPage);
    }

    public BookingResponse getBookingById(Long id, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        boolean admin = authenticatedUserService.isAdmin(authentication);
        Booking booking = findBookingById(id);

        validateBookingAccess(booking, currentUser, admin);
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        boolean admin = authenticatedUserService.isAdmin(authentication);
        Resource resource = findResourceById(request.getResourceId());
        validateBookingConflict(resource.getId(), request.getStartTime(), request.getEndTime(), null);

        BookingOwnership ownership = resolveOwnershipForCreate(currentUser, admin, request.getRequesterId());

        Booking booking = Booking.builder()
                .resource(resource)
                .ownerUser(ownership.ownerUser())
                .requesterId(ownership.requesterId())
                .purpose(normalizeRequiredText(request.getPurpose()))
                .expectedAttendees(request.getExpectedAttendees())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.PENDING)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        auditLogService.recordEvent(
                "BOOKING",
                savedBooking.getId(),
                "BOOKING_CREATED",
                currentUser,
                currentUser.getEmail(),
                "Booking created for resource " + savedBooking.getResource().getResourceCode()
                        + " with status " + savedBooking.getStatus() + "."
        );
        notificationService.notifyBookingCreated(savedBooking);
        return BookingResponse.from(savedBooking);
    }

    @Transactional
    public BookingResponse updateBooking(Long id, UpdateBookingRequest request, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        boolean admin = authenticatedUserService.isAdmin(authentication);
        Booking booking = findBookingById(id);

        validateBookingAccess(booking, currentUser, admin);
        validateBookingCanBeEdited(booking);

        Resource resource = findResourceById(request.getResourceId());
        validateBookingConflict(resource.getId(), request.getStartTime(), request.getEndTime(), booking.getId());

        BookingOwnership ownership = resolveOwnershipForUpdate(booking, currentUser, admin, request.getRequesterId());

        booking.setResource(resource);
        booking.setOwnerUser(ownership.ownerUser());
        booking.setRequesterId(ownership.requesterId());
        booking.setPurpose(normalizeRequiredText(request.getPurpose()));
        booking.setExpectedAttendees(request.getExpectedAttendees());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse updateBookingStatus(Long id, UpdateBookingStatusRequest request, Authentication authentication) {
        requireAdmin(authentication);
        User currentUser = authenticatedUserService.getCurrentUser(authentication);

        Booking booking = findBookingById(id);
        BookingStatus currentStatus = booking.getStatus();
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

        Booking savedBooking = bookingRepository.save(booking);
        auditLogService.recordEvent(
                "BOOKING",
                savedBooking.getId(),
                "BOOKING_" + targetStatus.name(),
                currentUser,
                currentUser.getEmail(),
                "Booking status changed from " + currentStatus + " to " + targetStatus
                        + (StringUtils.hasText(savedBooking.getAdminDecisionReason())
                        ? ". Reason: " + savedBooking.getAdminDecisionReason()
                        : ".")
        );
        notificationService.notifyBookingStatusChanged(savedBooking);
        return BookingResponse.from(savedBooking);
    }

    @Transactional
    public void deleteBooking(Long id, Authentication authentication) {
        User currentUser = authenticatedUserService.getCurrentUser(authentication);
        boolean admin = authenticatedUserService.isAdmin(authentication);
        Booking booking = findBookingById(id);

        validateBookingAccess(booking, currentUser, admin);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        BookingStatus previousStatus = booking.getStatus();
        validateStatusTransition(previousStatus, BookingStatus.CANCELLED);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setAdminDecisionReason(null);
        Booking savedBooking = bookingRepository.save(booking);
        auditLogService.recordEvent(
                "BOOKING",
                savedBooking.getId(),
                "BOOKING_CANCELLED",
                currentUser,
                currentUser.getEmail(),
                "Booking cancelled from previous status " + previousStatus + "."
        );
        notificationService.notifyBookingStatusChanged(savedBooking != null ? savedBooking : booking);
    }

    private Booking findBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
    }

    private Resource findResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    private void requireAdmin(Authentication authentication) {
        if (!authenticatedUserService.isAdmin(authentication)) {
            throw new AccessDeniedException("Admin access is required.");
        }
    }

    private void validateBookingAccess(Booking booking, User currentUser, boolean admin) {
        if (admin || isOwnedBy(booking, currentUser)) {
            return;
        }

        throw new AccessDeniedException("You can only access your own bookings.");
    }

    private boolean isOwnedBy(Booking booking, User user) {
        if (booking.getOwnerUser() != null && booking.getOwnerUser().getEmail() != null
                && booking.getOwnerUser().getEmail().equalsIgnoreCase(user.getEmail())) {
            return true;
        }

        return StringUtils.hasText(booking.getRequesterId())
                && booking.getRequesterId().trim().equalsIgnoreCase(user.getEmail());
    }

    private String resolveVisibleRequesterFilter(User currentUser, boolean admin, String requesterId) {
        if (admin) {
            return normalizeNullableText(requesterId);
        }

        if (StringUtils.hasText(requesterId) && !requesterId.trim().equalsIgnoreCase(currentUser.getEmail())) {
            throw new AccessDeniedException("You can only view your own bookings.");
        }

        return currentUser.getEmail();
    }

    private BookingOwnership resolveOwnershipForCreate(User currentUser, boolean admin, String requesterId) {
        if (!admin) {
            validateRequestedIdentifierMatchesCurrentUser(requesterId, currentUser, "create bookings");
            return new BookingOwnership(currentUser, currentUser.getEmail());
        }

        if (!StringUtils.hasText(requesterId)) {
            return new BookingOwnership(currentUser, currentUser.getEmail());
        }

        return mapOwnershipFromIdentifier(normalizeRequiredText(requesterId));
    }

    private BookingOwnership resolveOwnershipForUpdate(
            Booking booking,
            User currentUser,
            boolean admin,
            String requesterId
    ) {
        if (!admin) {
            validateRequestedIdentifierMatchesCurrentUser(requesterId, currentUser, "update bookings");
            return new BookingOwnership(currentUser, currentUser.getEmail());
        }

        if (!StringUtils.hasText(requesterId)) {
            return new BookingOwnership(booking.getOwnerUser(), booking.getRequesterId());
        }

        return mapOwnershipFromIdentifier(normalizeRequiredText(requesterId));
    }

    private BookingOwnership mapOwnershipFromIdentifier(String requesterId) {
        User ownerUser = userRepository.findByEmailIgnoreCase(requesterId).orElse(null);
        return new BookingOwnership(ownerUser, ownerUser != null ? ownerUser.getEmail() : requesterId);
    }

    private void validateRequestedIdentifierMatchesCurrentUser(String requesterId, User currentUser, String action) {
        if (StringUtils.hasText(requesterId) && !requesterId.trim().equalsIgnoreCase(currentUser.getEmail())) {
            throw new AccessDeniedException("You can only " + action + " for your own account.");
        }
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

    private String normalizeRequiredText(String value) {
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record BookingOwnership(User ownerUser, String requesterId) {
    }
}
