package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.NotificationResponse;
import com.sliit.paf.smart_campus.exception.NotificationNotFoundException;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.BookingStatus;
import com.sliit.paf.smart_campus.model.Notification;
import com.sliit.paf.smart_campus.model.NotificationType;
import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketStatus;
import com.sliit.paf.smart_campus.model.User;
import com.sliit.paf.smart_campus.repository.NotificationRepository;
import com.sliit.paf.smart_campus.repository.NotificationSpecifications;
import com.sliit.paf.smart_campus.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final String BOOKING_ENTITY = "BOOKING";
    private static final String TICKET_ENTITY = "TICKET";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<NotificationResponse> getNotifications(String recipientIdentifier, Boolean unreadOnly, String type) {
        return notificationRepository.findAll(
                        NotificationSpecifications.hasRecipient(normalizeIdentifier(recipientIdentifier))
                                .and(NotificationSpecifications.hasUnreadOnly(unreadOnly))
                                .and(NotificationSpecifications.hasType(parseType(type))),
                        Sort.by(Sort.Direction.DESC, "createdAt")
                ).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotifications(String recipientIdentifier) {
        return getNotifications(recipientIdentifier, true, null);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, String recipientIdentifier) {
        Notification notification = findNotificationForRecipient(id, recipientIdentifier);

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllAsRead(String recipientIdentifier) {
        List<Notification> notifications = notificationRepository.findAll(
                NotificationSpecifications.hasRecipient(normalizeIdentifier(recipientIdentifier))
                        .and(NotificationSpecifications.hasUnreadOnly(true)),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(now);
        });

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    @Transactional
    public void deleteNotification(Long id, String recipientIdentifier) {
        Notification notification = findNotificationForRecipient(id, recipientIdentifier);
        notificationRepository.delete(notification);
    }

    @Transactional
    public void notifyBookingCreated(Booking booking) {
        createNotification(
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

        createNotification(booking.getRequesterId(), type, title, message, BOOKING_ENTITY, booking.getId());
    }

    @Transactional
    public void notifyTicketCreated(Ticket ticket) {
        createNotification(
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
                ticket.getAssignedTechnician(),
                NotificationType.TICKET_ASSIGNED,
                "Ticket assigned",
                "You were assigned to ticket \"" + ticket.getTitle() + "\".",
                TICKET_ENTITY,
                ticket.getId()
        );

        createNotification(
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

        createNotification(ticket.getReportedBy(), type, title, message, TICKET_ENTITY, ticket.getId());
    }

    @Transactional
    public NotificationResponse createGeneralNotification(String recipientIdentifier, String title, String message) {
        return NotificationResponse.from(createNotification(
                recipientIdentifier,
                NotificationType.GENERAL,
                title,
                message,
                "GENERAL",
                null
        ));
    }

    private Notification findNotificationForRecipient(Long id, String recipientIdentifier) {
        return notificationRepository.findByIdAndRecipientIdentifierIgnoreCase(id, normalizeIdentifier(recipientIdentifier))
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + id));
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
                createNotification(recipient, type, title, message, relatedEntityType, relatedEntityId));
    }

    private Notification createNotification(
            String recipientIdentifier,
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            Long relatedEntityId
    ) {
        if (!StringUtils.hasText(recipientIdentifier)) {
            return null;
        }

        String normalizedRecipient = normalizeIdentifier(recipientIdentifier);
        Notification notification = Notification.builder()
                .recipientIdentifier(normalizedRecipient)
                .recipientUser(userRepository.findByEmailIgnoreCase(normalizedRecipient).orElse(null))
                .title(title.trim())
                .message(message.trim())
                .type(type)
                .relatedEntityType(StringUtils.hasText(relatedEntityType) ? relatedEntityType.trim().toUpperCase(Locale.ROOT) : null)
                .relatedEntityId(relatedEntityId)
                .isRead(false)
                .build();

        return notificationRepository.save(notification);
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
}
