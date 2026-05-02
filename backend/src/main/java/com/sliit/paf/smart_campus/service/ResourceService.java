package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.CreateResourceRequest;
import com.sliit.paf.smart_campus.dto.ResourceResponse;
import com.sliit.paf.smart_campus.dto.UpdateResourceRequest;
import com.sliit.paf.smart_campus.exception.DuplicateResourceException;
import com.sliit.paf.smart_campus.exception.ResourceNotFoundException;
import com.sliit.paf.smart_campus.model.Resource;
import com.sliit.paf.smart_campus.model.ResourceStatus;
import com.sliit.paf.smart_campus.model.ResourceType;
import com.sliit.paf.smart_campus.repository.ResourceRepository;
import com.sliit.paf.smart_campus.repository.ResourceSpecifications;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<ResourceResponse> getAllResources(String type, String location, Integer minCapacity, String status) {
        return resourceRepository.findAll(
                        ResourceSpecifications.hasType(parseType(type))
                                .and(ResourceSpecifications.hasLocation(location))
                                .and(ResourceSpecifications.hasMinCapacity(minCapacity))
                                .and(ResourceSpecifications.hasStatus(parseStatus(status)))
                ).stream()
                .map(ResourceResponse::from)
                .toList();
    }

    public ResourceResponse getResourceById(Long id) {
        return ResourceResponse.from(findResourceById(id));
    }

    @Transactional
    public ResourceResponse createResource(CreateResourceRequest request) {
        String normalizedResourceCode = normalizeResourceCode(request.getResourceCode());
        validateUniqueResourceCode(normalizedResourceCode, null);

        Resource resource = Resource.builder()
                .resourceCode(normalizedResourceCode)
                .name(normalizeText(request.getName()))
                .description(normalizeNullableText(request.getDescription()))
                .type(ResourceType.from(request.getType()))
                .capacity(request.getCapacity())
                .location(normalizeText(request.getLocation()))
                .availabilityStart(request.getAvailabilityStart())
                .availabilityEnd(request.getAvailabilityEnd())
                .status(ResourceStatus.from(request.getStatus()))
                .build();

        return ResourceResponse.from(persistResource(resource, normalizedResourceCode));
    }

    @Transactional
    public ResourceResponse updateResource(Long id, UpdateResourceRequest request) {
        Resource resource = findResourceById(id);
        String normalizedResourceCode = normalizeResourceCode(request.getResourceCode());
        validateUniqueResourceCode(normalizedResourceCode, id);

        resource.setResourceCode(normalizedResourceCode);
        resource.setName(normalizeText(request.getName()));
        resource.setDescription(normalizeNullableText(request.getDescription()));
        resource.setType(ResourceType.from(request.getType()));
        resource.setCapacity(request.getCapacity());
        resource.setLocation(normalizeText(request.getLocation()));
        resource.setAvailabilityStart(request.getAvailabilityStart());
        resource.setAvailabilityEnd(request.getAvailabilityEnd());
        resource.setStatus(ResourceStatus.from(request.getStatus()));

        return ResourceResponse.from(persistResource(resource, normalizedResourceCode));
    }

    @Transactional
    public void deleteResource(Long id) {
        resourceRepository.delete(findResourceById(id));
    }

    private Resource findResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    private void validateUniqueResourceCode(String resourceCode, Long currentResourceId) {
        boolean resourceCodeExists = currentResourceId == null
                ? resourceRepository.existsByResourceCodeIgnoreCase(resourceCode)
                : resourceRepository.existsByResourceCodeIgnoreCaseAndIdNot(resourceCode, currentResourceId);

        if (resourceCodeExists) {
            throw new DuplicateResourceException("Resource code already exists: " + resourceCode);
        }
    }

    private Resource persistResource(Resource resource, String resourceCode) {
        try {
            return resourceRepository.save(resource);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("Resource code already exists: " + resourceCode);
        }
    }

    private ResourceType parseType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return ResourceType.from(type);
    }

    private ResourceStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return ResourceStatus.from(status);
    }

    private String normalizeResourceCode(String resourceCode) {
        return normalizeText(resourceCode).toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
