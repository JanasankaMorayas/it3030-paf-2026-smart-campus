package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.dto.CreateResourceRequest;
import com.sliit.paf.smart_campus.dto.UpdateResourceRequest;
import com.sliit.paf.smart_campus.model.Resource;
import com.sliit.paf.smart_campus.model.ResourceStatus;
import com.sliit.paf.smart_campus.model.ResourceType;
import com.sliit.paf.smart_campus.repository.BookingRepository;
import com.sliit.paf.smart_campus.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        resourceRepository.deleteAll();
    }

    @Test
    void createResource_shouldReturnCreatedWhenRequestIsValid() throws Exception {
        CreateResourceRequest request = CreateResourceRequest.builder()
                .resourceCode("LAB-201")
                .name("Data Science Lab")
                .description("Lab with 50 seats")
                .type("LAB")
                .capacity(50)
                .location("Block A")
                .availabilityStart(LocalDateTime.of(2026, 4, 22, 8, 0))
                .availabilityEnd(LocalDateTime.of(2026, 4, 22, 18, 0))
                .status("ACTIVE")
                .build();

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceCode").value("LAB-201"))
                .andExpect(jsonPath("$.name").value("Data Science Lab"))
                .andExpect(jsonPath("$.type").value("LAB"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getAllResources_shouldReturnOk() throws Exception {
        resourceRepository.save(Resource.builder()
                .resourceCode("EQ-001")
                .name("Portable Projector")
                .description("Full HD projector")
                .type(ResourceType.EQUIPMENT)
                .capacity(1)
                .location("Media Unit")
                .status(ResourceStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resourceCode").value("EQ-001"))
                .andExpect(jsonPath("$[0].type").value("EQUIPMENT"));
    }

    @Test
    void getResourceById_shouldReturnNotFoundWhenIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/resources/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Resource not found with id: 999"));
    }

    @Test
    void getResourceById_shouldReturnResourceWhenIdExists() throws Exception {
        Resource savedResource = resourceRepository.save(Resource.builder()
                .resourceCode("HALL-101")
                .name("Main Lecture Hall")
                .description("Large lecture hall")
                .type(ResourceType.LECTURE_HALL)
                .capacity(120)
                .location("Block C")
                .status(ResourceStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/resources/{id}", savedResource.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedResource.getId()))
                .andExpect(jsonPath("$.resourceCode").value("HALL-101"))
                .andExpect(jsonPath("$.type").value("LECTURE_HALL"));
    }

    @Test
    void createResource_shouldReturnBadRequestWhenValidationFails() throws Exception {
        CreateResourceRequest request = CreateResourceRequest.builder()
                .resourceCode("")
                .name("")
                .type("")
                .capacity(0)
                .location("")
                .status("")
                .build();

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.resourceCode").value("Resource code is required."))
                .andExpect(jsonPath("$.validationErrors.name").value("Resource name is required."))
                .andExpect(jsonPath("$.validationErrors.capacity").value("Capacity must be at least 1."));
    }

    @Test
    void createResource_shouldReturnBadRequestWhenEnumOrAvailabilityRangeIsInvalid() throws Exception {
        CreateResourceRequest request = CreateResourceRequest.builder()
                .resourceCode("LAB-999")
                .name("Broken Request")
                .type("INVALID_TYPE")
                .capacity(20)
                .location("Block Z")
                .availabilityStart(LocalDateTime.of(2026, 4, 22, 18, 0))
                .availabilityEnd(LocalDateTime.of(2026, 4, 22, 8, 0))
                .status("INVALID_STATUS")
                .build();

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.type").value("Resource type must be one of: LECTURE_HALL, LAB, MEETING_ROOM, EQUIPMENT."))
                .andExpect(jsonPath("$.validationErrors.status").value("Resource status must be one of: ACTIVE, OUT_OF_SERVICE."))
                .andExpect(jsonPath("$.validationErrors.availabilityEnd").value("Availability end must be after or equal to availability start."));
    }

    @Test
    void createResource_shouldReturnConflictWhenResourceCodeAlreadyExists() throws Exception {
        resourceRepository.save(Resource.builder()
                .resourceCode("LAB-201")
                .name("Existing Lab")
                .description("Already stored resource")
                .type(ResourceType.LAB)
                .capacity(35)
                .location("Block A")
                .status(ResourceStatus.ACTIVE)
                .build());

        CreateResourceRequest request = CreateResourceRequest.builder()
                .resourceCode("LAB-201")
                .name("Duplicate Lab")
                .description("Should fail")
                .type("LAB")
                .capacity(40)
                .location("Block B")
                .status("ACTIVE")
                .build();

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Resource code already exists: LAB-201"));
    }

    @Test
    void searchResources_shouldReturnFilteredResults() throws Exception {
        resourceRepository.save(Resource.builder()
                .resourceCode("LAB-301")
                .name("Networking Lab")
                .description("Filtered resource")
                .type(ResourceType.LAB)
                .capacity(45)
                .location("Block A")
                .status(ResourceStatus.ACTIVE)
                .build());

        resourceRepository.save(Resource.builder()
                .resourceCode("ROOM-101")
                .name("Meeting Room")
                .description("Non matching resource")
                .type(ResourceType.MEETING_ROOM)
                .capacity(12)
                .location("Block B")
                .status(ResourceStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/resources/search")
                        .param("type", "LAB")
                        .param("location", "Block A")
                        .param("minCapacity", "40")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].resourceCode").value("LAB-301"))
                .andExpect(jsonPath("$[0].type").value("LAB"));
    }

    @Test
    void getAllResources_shouldApplyOptionalFilters() throws Exception {
        resourceRepository.save(Resource.builder()
                .resourceCode("LAB-450")
                .name("Software Lab")
                .description("Matching resource")
                .type(ResourceType.LAB)
                .capacity(45)
                .location("Block A")
                .status(ResourceStatus.ACTIVE)
                .build());

        resourceRepository.save(Resource.builder()
                .resourceCode("EQ-450")
                .name("Audio Kit")
                .description("Different type")
                .type(ResourceType.EQUIPMENT)
                .capacity(1)
                .location("Block A")
                .status(ResourceStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/resources")
                        .param("type", "LAB")
                        .param("location", "Block A")
                        .param("minCapacity", "40")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].resourceCode").value("LAB-450"));
    }

    @Test
    void updateResource_shouldReturnUpdatedResource() throws Exception {
        Resource savedResource = resourceRepository.save(Resource.builder()
                .resourceCode("ROOM-201")
                .name("Old Meeting Room")
                .description("Old description")
                .type(ResourceType.MEETING_ROOM)
                .capacity(12)
                .location("Block B")
                .status(ResourceStatus.ACTIVE)
                .build());

        UpdateResourceRequest updateRequest = UpdateResourceRequest.builder()
                .resourceCode("ROOM-202")
                .name("Updated Meeting Room")
                .description("Updated description")
                .type("MEETING_ROOM")
                .capacity(16)
                .location("Block D")
                .availabilityStart(LocalDateTime.of(2026, 4, 23, 9, 0))
                .availabilityEnd(LocalDateTime.of(2026, 4, 23, 17, 0))
                .status("OUT_OF_SERVICE")
                .build();

        mockMvc.perform(put("/api/resources/{id}", savedResource.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceCode").value("ROOM-202"))
                .andExpect(jsonPath("$.name").value("Updated Meeting Room"))
                .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
    }

    @Test
    void deleteResource_shouldReturnNoContent() throws Exception {
        Resource savedResource = resourceRepository.save(Resource.builder()
                .resourceCode("EQ-999")
                .name("Delete Me")
                .description("To be removed")
                .type(ResourceType.EQUIPMENT)
                .capacity(1)
                .location("Storage")
                .status(ResourceStatus.ACTIVE)
                .build());

        mockMvc.perform(delete("/api/resources/{id}", savedResource.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/resources/{id}", savedResource.getId()))
                .andExpect(status().isNotFound());
    }
}
