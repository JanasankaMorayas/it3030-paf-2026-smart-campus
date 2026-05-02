package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.dto.CreateResourceRequest;
import com.sliit.paf.smart_campus.dto.ResourceResponse;
import com.sliit.paf.smart_campus.dto.UpdateResourceRequest;
import com.sliit.paf.smart_campus.service.ResourceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@Validated
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getAllResources(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @Min(value = 1, message = "minCapacity must be at least 1.") Integer minCapacity,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(resourceService.getAllResources(type, location, minCapacity, status));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceResponse>> searchResources(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @Min(value = 1, message = "minCapacity must be at least 1.") Integer minCapacity,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(resourceService.getAllResources(type, location, minCapacity, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @PostMapping
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody CreateResourceRequest resource) {
        ResourceResponse createdResource = resourceService.createResource(resource);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdResource);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody UpdateResourceRequest resourceDetails
    ) {
        return ResponseEntity.ok(resourceService.updateResource(id, resourceDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
