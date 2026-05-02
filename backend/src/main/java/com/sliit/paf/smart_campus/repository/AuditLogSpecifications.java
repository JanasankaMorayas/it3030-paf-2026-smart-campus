package com.sliit.paf.smart_campus.repository;

import com.sliit.paf.smart_campus.model.AuditLog;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> hasEntityType(String entityType) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(entityType)) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("entityType")),
                    entityType.trim().toLowerCase(Locale.ROOT)
            );
        };
    }

    public static Specification<AuditLog> hasEntityId(Long entityId) {
        return (root, query, criteriaBuilder) ->
                entityId == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditLog> hasAction(String action) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(action)) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("action")),
                    action.trim().toLowerCase(Locale.ROOT)
            );
        };
    }

    public static Specification<AuditLog> hasPerformedBy(String performedBy) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(performedBy)) {
                return criteriaBuilder.conjunction();
            }

            String normalized = performedBy.trim().toLowerCase(Locale.ROOT);
            return criteriaBuilder.or(
                    criteriaBuilder.equal(criteriaBuilder.lower(root.get("performedByIdentifier")), normalized),
                    criteriaBuilder.equal(
                            criteriaBuilder.lower(root.join("performedByUser", JoinType.LEFT).get("email")),
                            normalized
                    )
            );
        };
    }

    public static Specification<AuditLog> createdAtOnOrAfter(LocalDateTime from) {
        return (root, query, criteriaBuilder) ->
                from == null ? criteriaBuilder.conjunction() : criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<AuditLog> createdAtOnOrBefore(LocalDateTime to) {
        return (root, query, criteriaBuilder) ->
                to == null ? criteriaBuilder.conjunction() : criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
