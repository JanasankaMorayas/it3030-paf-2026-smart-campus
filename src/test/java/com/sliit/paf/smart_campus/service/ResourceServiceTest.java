package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.CreateResourceRequest;
import com.sliit.paf.smart_campus.dto.ResourceResponse;
import com.sliit.paf.smart_campus.dto.UpdateResourceRequest;
import com.sliit.paf.smart_campus.exception.DuplicateResourceException;
import com.sliit.paf.smart_campus.model.Resource;
import com.sliit.paf.smart_campus.model.ResourceStatus;
import com.sliit.paf.smart_campus.model.ResourceType;
import com.sliit.paf.smart_campus.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    void createResource_shouldSaveResourceAndReturnResponse() {
        CreateResourceRequest request = CreateResourceRequest.builder()
                .resourceCode("lab-001")
                .name("Advanced Networks Lab")
                .description("High-capacity networking lab")
                .type("LAB")
                .capacity(40)
                .location("Block A")
                .availabilityStart(LocalDateTime.of(2026, 4, 22, 8, 0))
                .availabilityEnd(LocalDateTime.of(2026, 4, 22, 18, 0))
                .status("ACTIVE")
                .build();

        Resource savedResource = Resource.builder()
                .id(1L)
                .resourceCode("LAB-001")
                .name("Advanced Networks Lab")
                .description("High-capacity networking lab")
                .type(ResourceType.LAB)
                .capacity(40)
                .location("Block A")
                .availabilityStart(request.getAvailabilityStart())
                .availabilityEnd(request.getAvailabilityEnd())
                .status(ResourceStatus.ACTIVE)
                .build();

        when(resourceRepository.existsByResourceCodeIgnoreCase("LAB-001")).thenReturn(false);
        when(resourceRepository.save(any(Resource.class))).thenReturn(savedResource);

        ResourceResponse response = resourceService.createResource(request);

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceRepository).save(resourceCaptor.capture());

        Resource capturedResource = resourceCaptor.getValue();
        assertThat(capturedResource.getResourceCode()).isEqualTo("LAB-001");
        assertThat(capturedResource.getType()).isEqualTo(ResourceType.LAB);
        assertThat(capturedResource.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getResourceCode()).isEqualTo("LAB-001");
    }

    @Test
    void updateResource_shouldUpdateExistingResource() {
        UpdateResourceRequest request = UpdateResourceRequest.builder()
                .resourceCode("lab-002")
                .name("AI Laboratory")
                .description("Updated description")
                .type("LAB")
                .capacity(60)
                .location("Block B")
                .availabilityStart(LocalDateTime.of(2026, 4, 23, 9, 0))
                .availabilityEnd(LocalDateTime.of(2026, 4, 23, 17, 0))
                .status("OUT_OF_SERVICE")
                .build();

        Resource existingResource = Resource.builder()
                .id(2L)
                .resourceCode("LAB-001")
                .name("Old Lab")
                .description("Old description")
                .type(ResourceType.LAB)
                .capacity(30)
                .location("Block A")
                .status(ResourceStatus.ACTIVE)
                .build();

        Resource updatedResource = Resource.builder()
                .id(2L)
                .resourceCode("LAB-002")
                .name("AI Laboratory")
                .description("Updated description")
                .type(ResourceType.LAB)
                .capacity(60)
                .location("Block B")
                .availabilityStart(request.getAvailabilityStart())
                .availabilityEnd(request.getAvailabilityEnd())
                .status(ResourceStatus.OUT_OF_SERVICE)
                .build();

        when(resourceRepository.findById(2L)).thenReturn(Optional.of(existingResource));
        when(resourceRepository.existsByResourceCodeIgnoreCaseAndIdNot("LAB-002", 2L)).thenReturn(false);
        when(resourceRepository.save(existingResource)).thenReturn(updatedResource);

        ResourceResponse response = resourceService.updateResource(2L, request);

        assertThat(existingResource.getResourceCode()).isEqualTo("LAB-002");
        assertThat(existingResource.getName()).isEqualTo("AI Laboratory");
        assertThat(existingResource.getStatus()).isEqualTo(ResourceStatus.OUT_OF_SERVICE);
        assertThat(response.getCapacity()).isEqualTo(60);
        verify(resourceRepository).save(existingResource);
    }

    @Test
    void deleteResource_shouldDeleteExistingResource() {
        Resource existingResource = Resource.builder()
                .id(3L)
                .resourceCode("EQ-100")
                .name("Projector")
                .type(ResourceType.EQUIPMENT)
                .capacity(1)
                .location("Block C")
                .status(ResourceStatus.ACTIVE)
                .build();

        when(resourceRepository.findById(3L)).thenReturn(Optional.of(existingResource));

        resourceService.deleteResource(3L);

        verify(resourceRepository).delete(existingResource);
    }

    @Test
    void createResource_shouldThrowConflictWhenResourceCodeAlreadyExists() {
        CreateResourceRequest request = CreateResourceRequest.builder()
                .resourceCode("lab-001")
                .name("Duplicate Lab")
                .type("LAB")
                .capacity(30)
                .location("Block D")
                .status("ACTIVE")
                .build();

        when(resourceRepository.existsByResourceCodeIgnoreCase("LAB-001")).thenReturn(true);

        assertThatThrownBy(() -> resourceService.createResource(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Resource code already exists: LAB-001");
    }

    @Test
    void createResource_shouldTranslateDatabaseConflictWhenUniqueConstraintFails() {
        CreateResourceRequest request = CreateResourceRequest.builder()
                .resourceCode("lab-003")
                .name("Concurrent Lab")
                .type("LAB")
                .capacity(32)
                .location("Block E")
                .status("ACTIVE")
                .build();

        when(resourceRepository.existsByResourceCodeIgnoreCase("LAB-003")).thenReturn(false);
        when(resourceRepository.save(any(Resource.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> resourceService.createResource(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Resource code already exists: LAB-003");
    }
}
