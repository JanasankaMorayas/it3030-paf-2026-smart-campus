package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.NotificationResponse;
import com.sliit.paf.smart_campus.dto.PageResponse;
import com.sliit.paf.smart_campus.exception.NotificationNotFoundException;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.Notification;
import com.sliit.paf.smart_campus.model.NotificationType;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketStatus;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.NotificationRepository;
import com.sliit.paf.smart_campus.repository.NotificationSpecifications;
import com.sliit.paf.smart_campus.repository.UserRepository;
import com.sliit.paf.smart_campus.util.PageableUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final String BOOKING_ENTITY = "BOOKING";
    private static final String TICKET_ENTITY = "TICKET";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "type", "createdAt", "readAt", "relatedEntityType", "relatedEntityId"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"));

    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<NotificationResponse> getNotifications(
            User currentUser,
            Boolean unreadOnly,
            String type,
            String recipient,
            Pageable pageable
    ) {
        Pageable sanitizedPageable = PageableUtils.sanitize(pageable, DEFAULT_SORT, ALLOWED_SORT_FIELDS);

        Page<NotificationResponse> notificationPage = notificationRepository.findAll(
                        NotificationSpecifications.hasRecipient(resolveRecipientScope(currentUser, recipient))
                                .and(NotificationSpecifications.hasUnreadOnly(unreadOnly))
                                .and(NotificationSpecifications.hasType(parseType(type))),
                        sanitizedPageable
                ).map(NotificationResponse::from);

        return PageResponse.from(notificationPage);
    }

    public PageResponse<NotificationResponse> getUnreadNotifications(User currentUser, String recipient, Pageable pageable) {
        return getNotifications(currentUser, true, null, recipient, pageable);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, User currentUser) {
        Notification notification = findAccessibleNotification(id, currentUser);

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllAsRead(User currentUser, String recipient) {
        List<Notification> notifications = notificationRepository.findAll(
                NotificationSpecifications.hasRecipient(resolveRecipientScope(currentUser, recipient))
                        .and(NotificationSpecifications.hasUnreadOnly(true)),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(notification -> {
            validateNotificationAccess(notification, currentUser);
            notification.setIsRead(true);
            notification.setReadAt(now);
        });

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }

        auditLogService.recordEvent(
                "NOTIFICATION",
                null,
                "NOTIFICATIONS_READ_ALL",
                currentUser,
                currentUser.getEmail(),
                "Marked " + notifications.size() + " notifications as read for recipient "
                        + resolveRecipientScope(currentUser, recipient) + "."
        );

        return notifications.size();
    }

    @Transactional
    public void deleteNotification(Long id, User currentUser) {
        Notification notification = findAccessibleNotification(id, currentUser);
        notificationRepository.delete(notification);
    }

    @Transactional
    public void notifyBookingCreated(Booking booking) {
        createNotification(
                booking.getOwnerUser(),
                booking.getRequesterId(),
                NotificationType.BOOKING_CREATED,
                "Booking request submitted",
                "Your booking request for " + booking.getResource().getName() + " is now pending review.",
                BOOKING_ENTITY,
                booking.getId()
        );

        notifyAdminUsers(
                NotificationType.BOOKING_CREATED,
                "New booking request",
                "A new booking request was submitted for " + booking.getResource().getName() + ".",
                BOOKING_ENTITY,
                booking.getId()
        );
    }

    @Transactional
    public void notifyBookingStatusChanged(Booking booking) {
        NotificationType type = switch (booking.getStatus()) {
            case APPROVED -> NotificationType.BOOKING_APPROVED;
            case REJECTED -> NotificationType.BOOKING_REJECTED;
            case CANCELLED -> NotificationType.BOOKING_CANCELLED;
            case PENDING -> NotificationType.BOOKING_CREATED;
        };

        String title = switch (booking.getStatus()) {
            case APPROVED -> "Booking approved";
            case REJECTED -> "Booking rejected";
            case CANCELLED -> "Booking cancelled";
            case PENDING -> "Booking request submitted";
        };

        String message = switch (booking.getStatus()) {
            case APPROVED -> "Your booking for " + booking.getResource().getName() + " was approved.";
            case REJECTED -> "Your booking for " + booking.getResource().getName() + " was rejected.";
            case CANCELLED -> "Your booking for " + booking.getResource().getName() + " was cancelled.";
            case PENDING -> "Your booking request for " + booking.getResource().getName() + " is pending review.";
        };

        createNotification(booking.getOwnerUser(), booking.getRequesterId(), type, title, message, BOOKING_ENTITY, booking.getId());
    }

    @Transactional
    public void notifyTicketCreated(Ticket ticket) {
        createNotification(
                ticket.getReportedByUser(),
                ticket.getReportedBy(),
                NotificationType.TICKET_CREATED,
                "Ticket created",
                "Your ticket \"" + ticket.getTitle() + "\" was created successfully.",
                TICKET_ENTITY,
                ticket.getId()
        );

        notifyAdminUsers(
                NotificationType.TICKET_CREATED,
                "New ticket reported",
                "A new ticket was reported at " + ticket.getLocation() + ".",
                TICKET_ENTITY,
                ticket.getId()
        );
    }

    @Transactional
    public void notifyTicketAssigned(Ticket ticket) {
        createNotification(
                ticket.getAssignedTechnicianUser(),
                ticket.getAssignedTechnician(),
                NotificationType.TICKET_ASSIGNED,
                "Ticket assigned",
                "You were assigned to ticket \"" + ticket.getTitle() + "\".",
                TICKET_ENTITY,
                ticket.getId()
        );

        createNotification(
                ticket.getReportedByUser(),
                ticket.getReportedBy(),
                NotificationType.TICKET_ASSIGNED,
                "Technician assigned",
                "A technician was assigned to your ticket \"" + ticket.getTitle() + "\".",
                TICKET_ENTITY,
                ticket.getId()
        );
    }

    @Transactional
    public void notifyTicketStatusChanged(Ticket ticket) {
        NotificationType type = ticket.getStatus() == TicketStatus.RESOLVED
                ? NotificationType.TICKET_RESOLVED
                : NotificationType.TICKET_STATUS_UPDATED;

        String title = ticket.getStatus() == TicketStatus.RESOLVED
                ? "Ticket resolved"
                : "Ticket status updated";

        String message = ticket.getStatus() == TicketStatus.RESOLVED
                ? "Your ticket \"" + ticket.getTitle() + "\" was resolved."
                : "Your ticket \"" + ticket.getTitle() + "\" is now " + ticket.getStatus().name() + ".";

        createNotification(ticket.getReportedByUser(), ticket.getReportedBy(), type, title, message, TICKET_ENTITY, ticket.getId());

        if (!sameRecipient(
                ticket.getReportedByUser(),
                ticket.getReportedBy(),
                ticket.getAssignedTechnicianUser(),
                ticket.getAssignedTechnician()
        )) {
            createNotification(
                    ticket.getAssignedTechnicianUser(),
                    ticket.getAssignedTechnician(),
                    type,
                    title,
                    "Assigned ticket \"" + ticket.getTitle() + "\" is now " + ticket.getStatus().name() + ".",
                    TICKET_ENTITY,
                    ticket.getId()
            );
        }
    }

    @Transactional
    public NotificationResponse createGeneralNotification(String recipientIdentifier, String title, String message) {
        return NotificationResponse.from(createNotification(
                userRepository.findByEmailIgnoreCase(normalizeIdentifier(recipientIdentifier)).orElse(null),
                recipientIdentifier,
                NotificationType.GENERAL,
                title,
                message,
                "GENERAL",
                null
        ));
    }

    private Notification findAccessibleNotification(Long id, User currentUser) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));

        validateNotificationAccess(notification, currentUser);
        return notification;
    }

    private void validateNotificationAccess(Notification notification, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        String currentUserEmail = normalizeIdentifier(currentUser.getEmail());
        if (notification.getRecipientUser() != null && StringUtils.hasText(notification.getRecipientUser().getEmail())
                && currentUserEmail.equals(normalizeIdentifier(notification.getRecipientUser().getEmail()))) {
            return;
        }

        if (StringUtils.hasText(notification.getRecipientIdentifier())
                && currentUserEmail.equals(normalizeIdentifier(notification.getRecipientIdentifier()))) {
            return;
        }

        throw new AccessDeniedException("You can only access your own notifications.");
    }

    private String resolveRecipientScope(User currentUser, String recipient) {
        if (currentUser.getRole() == Role.ADMIN) {
            return normalizeNullableIdentifier(recipient);
        }

        if (StringUtils.hasText(recipient) && !recipient.trim().equalsIgnoreCase(currentUser.getEmail())) {
            throw new AccessDeniedException("You can only view your own notifications.");
        }

        return normalizeIdentifier(currentUser.getEmail());
    }

    private void notifyAdminUsers(
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            Long relatedEntityId
    ) {
        Set<String> adminRecipients = new LinkedHashSet<>();
        userRepository.findAllByRoleAndActiveTrueOrderByIdAsc(Role.ADMIN)
                .stream()
                .map(User::getEmail)
                .filter(StringUtils::hasText)
                .map(this::normalizeIdentifier)
                .forEach(adminRecipients::add);

        adminRecipients.forEach(recipient ->
                createNotification(
                        userRepository.findByEmailIgnoreCase(recipient).orElse(null),
                        recipient,
                        type,
                        title,
                        message,
                        relatedEntityType,
                        relatedEntityId
                ));
    }

    private Notification createNotification(
            User recipientUser,
            String recipientIdentifier,
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            Long relatedEntityId
    ) {
        String resolvedIdentifier = resolveNotificationRecipientIdentifier(recipientUser, recipientIdentifier);
        if (!StringUtils.hasText(resolvedIdentifier)) {
            return null;
        }

        User resolvedRecipientUser = recipientUser != null
                ? recipientUser
                : userRepository.findByEmailIgnoreCase(resolvedIdentifier).orElse(null);

        Notification notification = Notification.builder()
                .recipientIdentifier(resolvedIdentifier)
                .recipientUser(resolvedRecipientUser)
                .title(title.trim())
                .message(message.trim())
                .type(type)
                .relatedEntityType(StringUtils.hasText(relatedEntityType) ? relatedEntityType.trim().toUpperCase(Locale.ROOT) : null)
                .relatedEntityId(relatedEntityId)
                .isRead(false)
                .build();

        return notificationRepository.save(notification);
    }

    private String resolveNotificationRecipientIdentifier(User recipientUser, String recipientIdentifier) {
        if (recipientUser != null && StringUtils.hasText(recipientUser.getEmail())) {
            return normalizeIdentifier(recipientUser.getEmail());
        }

        return normalizeNullableIdentifier(recipientIdentifier);
    }

    private boolean sameRecipient(
            User firstRecipientUser,
            String firstRecipientIdentifier,
            User secondRecipientUser,
            String secondRecipientIdentifier
    ) {
        return Objects.equals(
                resolveNotificationRecipientIdentifier(firstRecipientUser, firstRecipientIdentifier),
                resolveNotificationRecipientIdentifier(secondRecipientUser, secondRecipientIdentifier)
        );
    }

    private NotificationType parseType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return NotificationType.from(type);
    }

    private String normalizeIdentifier(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullableIdentifier(String value) {
        return StringUtils.hasText(value) ? normalizeIdentifier(value) : null;
    }
}
