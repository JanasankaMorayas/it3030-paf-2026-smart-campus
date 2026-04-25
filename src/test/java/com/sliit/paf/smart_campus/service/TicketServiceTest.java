package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.AssignTechnicianRequest;
import com.sliit.paf.smart_campus.dto.CreateTicketRequest;
import com.sliit.paf.smart_campus.dto.TicketResponse;
import com.sliit.paf.smart_campus.dto.UpdateTicketStatusRequest;
import com.sliit.paf.smart_campus.dto.UpdateTicketRequest;
import com.sliit.paf.smart_campus.exception.InvalidTicketStateException;
import com.sliit.paf.smart_campus.exception.TooManyAttachmentsException;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketCategory;
import com.sliit.paf.smart_campus.model.TicketPriority;
import com.sliit.paf.smart_campus.model.TicketStatus;
import com.sliit.paf.smart_campus.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void createTicket_shouldSaveOpenTicketWithImages() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Projector not working")
                .description("The projector in Hall A is flickering.")
                .category("ELECTRICAL")
                .priority("HIGH")
                .location("Hall A")
                .reportedBy("staff-1")
                .imageUrls(List.of("https://img.example.com/1.jpg", "https://img.example.com/2.jpg"))
                .build();

        Ticket savedTicket = Ticket.builder()
                .id(1L)
                .title("Projector not working")
                .description("The projector in Hall A is flickering.")
                .category(TicketCategory.ELECTRICAL)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .location("Hall A")
                .reportedBy("staff-1")
                .imageUrl1("https://img.example.com/1.jpg")
                .imageUrl2("https://img.example.com/2.jpg")
                .build();

        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);

        TicketResponse response = ticketService.createTicket(request);

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());

        Ticket capturedTicket = ticketCaptor.getValue();
        assertThat(capturedTicket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(capturedTicket.getImageUrl1()).isEqualTo("https://img.example.com/1.jpg");
        assertThat(response.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.getImageUrls()).containsExactly("https://img.example.com/1.jpg", "https://img.example.com/2.jpg");
    }

    @Test
    void createTicket_shouldThrowWhenMoreThanThreeImagesProvided() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Overflow attachments")
                .description("Too many images")
                .category("OTHER")
                .priority("LOW")
                .location("Block B")
                .reportedBy("student-1")
                .imageUrls(List.of("1", "2", "3", "4"))
                .build();

        assertThatThrownBy(() -> ticketService.createTicket(request))
                .isInstanceOf(TooManyAttachmentsException.class)
                .hasMessage("A maximum of 3 image URLs is allowed.");
    }

    @Test
    void updateTicket_shouldRejectResolvedTickets() {
        Ticket ticket = Ticket.builder()
                .id(2L)
                .title("Resolved ticket")
                .description("Already resolved")
                .category(TicketCategory.NETWORK)
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.RESOLVED)
                .location("Lab 1")
                .reportedBy("staff-2")
                .build();

        UpdateTicketRequest request = UpdateTicketRequest.builder()
                .title("Updated title")
                .description("Updated description")
                .category("NETWORK")
                .priority("HIGH")
                .location("Lab 2")
                .reportedBy("staff-3")
                .imageUrls(List.of())
                .build();

        when(ticketRepository.findById(2L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateTicket(2L, request))
                .isInstanceOf(InvalidTicketStateException.class)
                .hasMessage("Only open or in-progress tickets can be updated.");
    }

    @Test
    void updateTicketStatus_shouldRequireResolutionNotesWhenResolving() {
        Ticket ticket = Ticket.builder()
                .id(3L)
                .title("Need notes")
                .description("Resolution note check")
                .category(TicketCategory.PLUMBING)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.IN_PROGRESS)
                .location("Washroom")
                .reportedBy("staff-4")
                .build();

        UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                .status("RESOLVED")
                .build();

        when(ticketRepository.findById(3L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateTicketStatus(3L, request))
                .isInstanceOf(InvalidTicketStateException.class)
                .hasMessage("Resolution notes are required when resolving a ticket.");
    }

    @Test
    void assignTechnician_shouldUpdateOpenTicket() {
        Ticket ticket = Ticket.builder()
                .id(4L)
                .title("Assign me")
                .description("Technician assignment")
                .category(TicketCategory.SAFETY)
                .priority(TicketPriority.CRITICAL)
                .status(TicketStatus.OPEN)
                .location("Gate 2")
                .reportedBy("admin-1")
                .build();

        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .assignedTechnician("tech-1")
                .build();

        when(ticketRepository.findById(4L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        TicketResponse response = ticketService.assignTechnician(4L, request);

        assertThat(ticket.getAssignedTechnician()).isEqualTo("tech-1");
        assertThat(response.getAssignedTechnician()).isEqualTo("tech-1");
    }

    @Test
    void deleteTicket_shouldCancelOpenTicket() {
        Ticket ticket = Ticket.builder()
                .id(5L)
                .title("Cancel ticket")
                .description("Cancel flow")
                .category(TicketCategory.CLEANING)
                .priority(TicketPriority.LOW)
                .status(TicketStatus.OPEN)
                .location("Corridor")
                .reportedBy("student-4")
                .build();

        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));

        ticketService.deleteTicket(5L);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository).save(ticket);
    }
}
