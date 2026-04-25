package com.sliit.paf.smart_campus.repository;

import com.sliit.paf.smart_campus.model.Notification;
import com.sliit.paf.smart_campus.model.NotificationType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class NotificationSpecifications {

    private NotificationSpecifications() {
    }

    public static Specification<Notification> hasRecipient(String recipientIdentifier) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.lower(root.get("recipientIdentifier")),
                recipientIdentifier.toLowerCase()
        );
    }

    public static Specification<Notification> hasUnreadOnly(Boolean unreadOnly) {
        return (root, query, criteriaBuilder) -> {
            if (unreadOnly == null || !unreadOnly) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.isFalse(root.get("isRead"));
        };
    }

    public static Specification<Notification> hasType(NotificationType type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("type"), type);
        };
    }
}
