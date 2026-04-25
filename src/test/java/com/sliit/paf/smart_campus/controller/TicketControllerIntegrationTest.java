package com.sliit.paf.smart_campus.controller;

import com.sliit.paf.smart_campus.dto.AssignTechnicianRequest;
import com.sliit.paf.smart_campus.dto.CreateTicketRequest;
import com.sliit.paf.smart_campus.dto.UpdateTicketRequest;
import com.sliit.paf.smart_campus.dto.UpdateTicketStatusRequest;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketCategory;
import com.sliit.paf.smart_campus.model.TicketPriority;
import com.sliit.paf.smart_campus.model.TicketStatus;
import com.sliit.paf.smart_campus.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

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
class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    void createTicket_shouldReturnCreatedWhenRequestIsValid() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Water leak in Block A")
                .description("Water leaking near the entrance.")
                .category("PLUMBING")
                .priority("HIGH")
                .location("Block A Entrance")
                .reportedBy("staff-1")
                .imageUrls(List.of("https://img.example.com/leak-1.jpg"))
                .build();

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Water leak in Block A"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.imageUrls[0]").value("https://img.example.com/leak-1.jpg"));
    }

    @Test
    void getAllTickets_shouldApplyFilters() throws Exception {
        ticketRepository.save(Ticket.builder()
                .title("Network issue")
                .description("Wi-Fi unstable")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.CRITICAL)
                .status(TicketStatus.IN_PROGRESS)
                .location("Library")
                .reportedBy("student-1")
                .assignedTechnician("tech-1")
                .build());

        ticketRepository.save(Ticket.builder()
                .title("Dustbin full")
                .description("Need cleaning")
                .category(TicketCategory.CLEANING)
                .priority(TicketPriority.LOW)
                .status(TicketStatus.OPEN)
                .location("Cafeteria")
                .reportedBy("student-2")
                .assignedTechnician("tech-2")
                .build());

        mockMvc.perform(get("/api/tickets")
                        .param("status", "IN_PROGRESS")
                        .param("priority", "CRITICAL")
                        .param("category", "NETWORK")
                        .param("reportedBy", "student-1")
                        .param("assignedTechnician", "tech-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Network issue"));
    }

    @Test
    void getTicketById_shouldReturnNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/tickets/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found with id: 999"));
    }

    @Test
    void createTicket_shouldReturnBadRequestWhenValidationFails() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("")
                .description("")
                .category("INVALID")
                .priority("")
                .location("")
                .reportedBy("")
                .imageUrls(List.of("1", "2", "3", "4"))
                .build();

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").value("Title is required."))
                .andExpect(jsonPath("$.validationErrors.category").value("Ticket category must be one of: ELECTRICAL, PLUMBING, NETWORK, CLEANING, SAFETY, OTHER."))
                .andExpect(jsonPath("$.validationErrors.imageUrls").value("A maximum of 3 image URLs is allowed."));
    }

    @Test
    void updateTicket_shouldReturnConflictWhenTicketIsNotEditable() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Closed ticket")
                .description("Already resolved")
                .category(TicketCategory.SAFETY)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.CLOSED)
                .location("Car Park")
                .reportedBy("staff-2")
                .build());

        UpdateTicketRequest request = UpdateTicketRequest.builder()
                .title("Updated title")
                .description("Updated description")
                .category("SAFETY")
                .priority("CRITICAL")
                .location("Car Park")
                .reportedBy("staff-2")
                .imageUrls(List.of())
                .build();

        mockMvc.perform(put("/api/tickets/{id}", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only open or in-progress tickets can be updated."));
    }

    @Test
    void updateTicketStatus_shouldResolveTicketWhenRequestIsValid() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Broken switch")
                .description("Switch needs fixing")
                .category(TicketCategory.ELECTRICAL)
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.IN_PROGRESS)
                .location("Lecture Hall 2")
                .reportedBy("staff-3")
                .build());

        UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                .status("RESOLVED")
                .resolutionNotes("Switch replaced and tested.")
                .build();

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionNotes").value("Switch replaced and tested."));
    }

    @Test
    void updateTicketStatus_shouldReturnConflictWhenResolutionNotesMissing() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Router issue")
                .description("Router unstable")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.IN_PROGRESS)
                .location("IT Lab")
                .reportedBy("student-5")
                .build());

        UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                .status("RESOLVED")
                .build();

        mockMvc.perform(patch("/api/tickets/{id}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Resolution notes are required when resolving a ticket."));
    }

    @Test
    void assignTechnician_shouldReturnUpdatedTicket() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Floor cleaning")
                .description("Spill in corridor")
                .category(TicketCategory.CLEANING)
                .priority(TicketPriority.LOW)
                .status(TicketStatus.OPEN)
                .location("Block C")
                .reportedBy("staff-6")
                .build());

        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .assignedTechnician("tech-7")
                .build();

        mockMvc.perform(patch("/api/tickets/{id}/assign", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTechnician").value("tech-7"));
    }

    @Test
    void deleteTicket_shouldCancelTicket() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Minor safety issue")
                .description("Loose wire")
                .category(TicketCategory.SAFETY)
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .location("Block D")
                .reportedBy("student-7")
                .build());

        mockMvc.perform(delete("/api/tickets/{id}", ticket.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tickets/{id}", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
