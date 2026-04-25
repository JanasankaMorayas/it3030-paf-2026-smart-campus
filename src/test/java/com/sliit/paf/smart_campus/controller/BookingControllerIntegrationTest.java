package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.dto.CreateBookingRequest;
import com.sliit.paf.smart_campus.dto.UpdateBookingRequest;
import com.sliit.paf.smart_campus.dto.UpdateBookingStatusRequest;
import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.BookingStatus;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "dev-user@smartcampus.local", roles = "USER")
class BookingControllerIntegrationTest {

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
    void createBooking_shouldReturnCreatedWhenRequestIsValid() throws Exception {
        Resource resource = saveResource("LAB-101", "Software Engineering Lab");

        CreateBookingRequest request = CreateBookingRequest.builder()
                .resourceId(resource.getId())
                .requesterId("student-1")
                .purpose("Database lab session")
                .expectedAttendees(32)
                .startTime(LocalDateTime.of(2026, 4, 25, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 25, 11, 0))
                .build();

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceId").value(resource.getId()))
                .andExpect(jsonPath("$.requesterId").value("student-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createBooking_shouldReturnNotFoundWhenResourceDoesNotExist() throws Exception {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .resourceId(999L)
                .requesterId("student-2")
                .purpose("Missing resource booking")
                .expectedAttendees(20)
                .startTime(LocalDateTime.of(2026, 4, 25, 12, 0))
                .endTime(LocalDateTime.of(2026, 4, 25, 14, 0))
                .build();

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resource not found with id: 999"));
    }

    @Test
    void createBooking_shouldReturnConflictWhenBookingOverlaps() throws Exception {
        Resource resource = saveResource("LAB-102", "Networks Lab");
        bookingRepository.save(Booking.builder()
                .resource(resource)
                .requesterId("student-3")
                .purpose("Existing booking")
                .expectedAttendees(25)
                .startTime(LocalDateTime.of(2026, 4, 25, 10, 0))
                .endTime(LocalDateTime.of(2026, 4, 25, 12, 0))
                .status(BookingStatus.PENDING)
                .build());

        CreateBookingRequest request = CreateBookingRequest.builder()
                .resourceId(resource.getId())
                .requesterId("student-4")
                .purpose("Overlap booking")
                .expectedAttendees(20)
                .startTime(LocalDateTime.of(2026, 4, 25, 11, 0))
                .endTime(LocalDateTime.of(2026, 4, 25, 13, 0))
                .build();

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Booking time overlaps with an existing booking for resource id: " + resource.getId()));
    }

    @Test
    void getAllBookings_shouldApplyOptionalFilters() throws Exception {
        Resource resource = saveResource("LAB-103", "Data Lab");
        bookingRepository.save(Booking.builder()
                .resource(resource)
                .requesterId("student-5")
                .purpose("Matching booking")
                .expectedAttendees(15)
                .startTime(LocalDateTime.of(2026, 4, 26, 8, 0))
                .endTime(LocalDateTime.of(2026, 4, 26, 10, 0))
                .status(BookingStatus.APPROVED)
                .build());

        bookingRepository.save(Booking.builder()
                .resource(resource)
                .requesterId("student-6")
                .purpose("Non matching booking")
                .expectedAttendees(10)
                .startTime(LocalDateTime.of(2026, 4, 26, 11, 0))
                .endTime(LocalDateTime.of(2026, 4, 26, 12, 0))
                .status(BookingStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/bookings")
                        .param("resourceId", String.valueOf(resource.getId()))
                        .param("requesterId", "student-5")
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requesterId").value("student-5"))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    void updateBooking_shouldReturnUpdatedBooking() throws Exception {
        Resource originalResource = saveResource("LAB-104", "Original Lab");
        Resource updatedResource = saveResource("ROOM-201", "Meeting Room");
        Booking booking = bookingRepository.save(Booking.builder()
                .resource(originalResource)
                .requesterId("student-7")
                .purpose("Original session")
                .expectedAttendees(18)
                .startTime(LocalDateTime.of(2026, 4, 26, 9, 0))
                .endTime(LocalDateTime.of(2026, 4, 26, 10, 0))
                .status(BookingStatus.PENDING)
                .build());

        UpdateBookingRequest request = UpdateBookingRequest.builder()
                .resourceId(updatedResource.getId())
                .requesterId("student-7")
                .purpose("Updated session")
                .expectedAttendees(20)
                .startTime(LocalDateTime.of(2026, 4, 26, 13, 0))
                .endTime(LocalDateTime.of(2026, 4, 26, 15, 0))
                .build();

        mockMvc.perform(put("/api/bookings/{id}", booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value(updatedResource.getId()))
                .andExpect(jsonPath("$.purpose").value("Updated session"));
    }

    @Test
    @WithMockUser(username = "dev-admin@smartcampus.local", roles = "ADMIN")
    void updateBookingStatus_shouldApproveBooking() throws Exception {
        Resource resource = saveResource("LAB-105", "Approval Lab");
        Booking booking = bookingRepository.save(Booking.builder()
                .resource(resource)
                .requesterId("student-8")
                .purpose("Approval request")
                .expectedAttendees(12)
                .startTime(LocalDateTime.of(2026, 4, 27, 8, 0))
                .endTime(LocalDateTime.of(2026, 4, 27, 10, 0))
                .status(BookingStatus.PENDING)
                .build());

        UpdateBookingStatusRequest request = UpdateBookingStatusRequest.builder()
                .status("APPROVED")
                .adminDecisionReason("Approved by admin.")
                .build();

        mockMvc.perform(patch("/api/bookings/{id}/status", booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.adminDecisionReason").value("Approved by admin."));
    }

    @Test
    void deleteBooking_shouldCancelBooking() throws Exception {
        Resource resource = saveResource("LAB-106", "Cancel Lab");
        Booking booking = bookingRepository.save(Booking.builder()
                .resource(resource)
                .requesterId("student-9")
                .purpose("Cancel me")
                .expectedAttendees(16)
                .startTime(LocalDateTime.of(2026, 4, 27, 11, 0))
                .endTime(LocalDateTime.of(2026, 4, 27, 13, 0))
                .status(BookingStatus.APPROVED)
                .build());

        mockMvc.perform(delete("/api/bookings/{id}", booking.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/bookings/{id}", booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void createBooking_shouldReturnBadRequestWhenValidationFails() throws Exception {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .resourceId(0L)
                .requesterId("")
                .purpose("")
                .expectedAttendees(0)
                .startTime(LocalDateTime.of(2026, 4, 27, 15, 0))
                .endTime(LocalDateTime.of(2026, 4, 27, 14, 0))
                .build();

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.resourceId").value("Resource id must be at least 1."))
                .andExpect(jsonPath("$.validationErrors.requesterId").value("Requester id is required."))
                .andExpect(jsonPath("$.validationErrors.endTime").value("End time must be after start time."));
    }

    @Test
    @WithMockUser(username = "dev-admin@smartcampus.local", roles = "ADMIN")
    void updateBookingStatus_shouldReturnBadRequestWhenStatusIsInvalid() throws Exception {
        Resource resource = saveResource("LAB-107", "Status Lab");
        Booking booking = bookingRepository.save(Booking.builder()
                .resource(resource)
                .requesterId("student-10")
                .purpose("Invalid status check")
                .expectedAttendees(8)
                .startTime(LocalDateTime.of(2026, 4, 27, 16, 0))
                .endTime(LocalDateTime.of(2026, 4, 27, 17, 0))
                .status(BookingStatus.PENDING)
                .build());

        UpdateBookingStatusRequest request = UpdateBookingStatusRequest.builder()
                .status("INVALID")
                .build();

        mockMvc.perform(patch("/api/bookings/{id}/status", booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.status").value("Booking status must be one of: PENDING, APPROVED, REJECTED, CANCELLED."));
    }

    @Test
    void getBookingById_shouldReturnNotFoundWhenBookingDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/bookings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found with id: 999"));
    }

    private Resource saveResource(String resourceCode, String name) {
        return resourceRepository.save(Resource.builder()
                .resourceCode(resourceCode)
                .name(name)
                .description("Test resource")
                .type(ResourceType.LAB)
                .capacity(50)
                .location("Block A")
                .status(ResourceStatus.ACTIVE)
                .build());
    }
}
