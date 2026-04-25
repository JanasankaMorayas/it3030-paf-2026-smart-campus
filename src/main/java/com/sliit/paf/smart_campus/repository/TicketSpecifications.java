package com.sliit.paf.smart_campus.repository;

import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketCategory;
import com.sliit.paf.smart_campus.model.TicketPriority;
import com.sliit.paf.smart_campus.model.TicketStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return (root, query, criteriaBuilder) ->
                priority == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> hasCategory(TicketCategory category) {
        return (root, query, criteriaBuilder) ->
                category == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("category"), category);
    }

    public static Specification<Ticket> hasReportedBy(String reportedBy) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(reportedBy)) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("reportedBy")),
                    reportedBy.trim().toLowerCase(Locale.ROOT)
            );
        };
    }

    public static Specification<Ticket> hasAssignedTechnician(String assignedTechnician) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(assignedTechnician)) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("assignedTechnician")),
                    assignedTechnician.trim().toLowerCase(Locale.ROOT)
            );
        };
    }
}
