package com.sliit.paf.smart_campus.repository;

import com.sliit.paf.smart_campus.model.Resource;
import com.sliit.paf.smart_campus.model.ResourceStatus;
import com.sliit.paf.smart_campus.model.ResourceType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class ResourceSpecifications {

    private ResourceSpecifications() {
    }

    public static Specification<Resource> hasType(ResourceType type) {
        return (root, query, criteriaBuilder) ->
                type == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("type"), type);
    }

    public static Specification<Resource> hasLocation(String location) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(location)) {
                return criteriaBuilder.conjunction();
            }

            String normalizedLocation = "%" + location.trim().toLowerCase(Locale.ROOT) + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), normalizedLocation);
        };
    }

    public static Specification<Resource> hasMinCapacity(Integer minCapacity) {
        return (root, query, criteriaBuilder) ->
                minCapacity == null ? criteriaBuilder.conjunction() : criteriaBuilder.greaterThanOrEqualTo(root.get("capacity"), minCapacity);
    }

    public static Specification<Resource> hasStatus(ResourceStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("status"), status);
    }
}
