package com.sliit.paf.smart_campus.repository;

import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.BookingStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<Booking> hasResourceId(Long resourceId) {
        return (root, query, criteriaBuilder) ->
                resourceId == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("resource").get("id"), resourceId);
    }

    public static Specification<Booking> hasRequesterId(String requesterId) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(requesterId)) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("requesterId")),
                    requesterId.trim().toLowerCase(Locale.ROOT)
            );
        };
    }

    public static Specification<Booking> hasOwnerIdentifier(String ownerIdentifier) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(ownerIdentifier)) {
                return criteriaBuilder.conjunction();
            }

            String normalizedIdentifier = ownerIdentifier.trim().toLowerCase(Locale.ROOT);
            return criteriaBuilder.or(
                    criteriaBuilder.equal(
                            criteriaBuilder.lower(root.get("requesterId")),
                            normalizedIdentifier
                    ),
                    criteriaBuilder.equal(
                            criteriaBuilder.lower(root.join("ownerUser", JoinType.LEFT).get("email")),
                            normalizedIdentifier
                    )
            );
        };
    }

    public static Specification<Booking> hasStatus(BookingStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("status"), status);
    }
}
