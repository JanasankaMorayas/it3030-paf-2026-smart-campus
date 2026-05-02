package com.sliit.paf.smart_campus.repository;

import com.sliit.paf.smart_campus.model.Notification;
import com.sliit.paf.smart_campus.model.NotificationType;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class NotificationSpecifications {

    private NotificationSpecifications() {
    }

    public static Specification<Notification> hasRecipient(String recipientIdentifier) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(recipientIdentifier)) {
                return criteriaBuilder.conjunction();
            }

            String normalizedRecipient = recipientIdentifier.toLowerCase();
            return criteriaBuilder.or(
                    criteriaBuilder.equal(
                            criteriaBuilder.lower(root.get("recipientIdentifier")),
                            normalizedRecipient
                    ),
                    criteriaBuilder.equal(
                            criteriaBuilder.lower(root.join("recipientUser", JoinType.LEFT).get("email")),
                            normalizedRecipient
                    )
            );
        };
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
