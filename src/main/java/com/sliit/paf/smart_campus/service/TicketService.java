package com.sliit.paf.smart_campus.service;

import com.sliit.paf.smart_campus.dto.AssignTechnicianRequest;
import com.sliit.paf.smart_campus.dto.CreateTicketRequest;
import com.sliit.paf.smart_campus.dto.TicketResponse;
import com.sliit.paf.smart_campus.dto.UpdateTicketRequest;
import com.sliit.paf.smart_campus.dto.UpdateTicketStatusRequest;
import com.sliit.paf.smart_campus.exception.InvalidTicketStateException;
import com.sliit.paf.smart_campus.exception.TicketNotFoundException;
import com.sliit.paf.smart_campus.exception.TooManyAttachmentsException;
import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketCategory;
import com.sliit.paf.smart_campus.model.TicketPriority;
import com.sliit.paf.smart_campus.model.TicketStatus;
import com.sliit.paf.smart_campus.repository.TicketRepository;
import com.sliit.paf.smart_campus.repository.TicketSpecifications;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<TicketResponse> getAllTickets(
            String status,
            String priority,
            String category,
            String reportedBy,
            String assignedTechnician
    ) {
        return ticketRepository.findAll(
                        TicketSpecifications.hasStatus(parseStatus(status))
                                .and(TicketSpecifications.hasPriority(parsePriority(priority)))
                                .and(TicketSpecifications.hasCategory(parseCategory(category)))
                                .and(TicketSpecifications.hasReportedBy(reportedBy))
                                .and(TicketSpecifications.hasAssignedTechnician(assignedTechnician))
                ).stream()
                .map(TicketResponse::from)
                .toList();
    }

    public TicketResponse getTicketById(Long id) {
        return TicketResponse.from(findTicketById(id));
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        List<String> imageUrls = normalizeImageUrls(request.getImageUrls());

        Ticket ticket = Ticket.builder()
                .title(normalizeText(request.getTitle()))
                .description(normalizeText(request.getDescription()))
                .category(TicketCategory.from(request.getCategory()))
                .priority(TicketPriority.from(request.getPriority()))
                .status(TicketStatus.OPEN)
                .location(normalizeText(request.getLocation()))
                .reportedBy(normalizeText(request.getReportedBy()))
                .build();

        applyImageUrls(ticket, imageUrls);

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse updateTicket(Long id, UpdateTicketRequest request) {
        Ticket ticket = findTicketById(id);
        validateTicketCanBeUpdated(ticket);

        List<String> imageUrls = normalizeImageUrls(request.getImageUrls());

        ticket.setTitle(normalizeText(request.getTitle()));
        ticket.setDescription(normalizeText(request.getDescription()));
        ticket.setCategory(TicketCategory.from(request.getCategory()));
        ticket.setPriority(TicketPriority.from(request.getPriority()));
        ticket.setLocation(normalizeText(request.getLocation()));
        ticket.setReportedBy(normalizeText(request.getReportedBy()));
        applyImageUrls(ticket, imageUrls);

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse updateTicketStatus(Long id, UpdateTicketStatusRequest request) {
        Ticket ticket = findTicketById(id);
        TicketStatus targetStatus = TicketStatus.from(request.getStatus());
        String resolutionNotes = normalizeNullableText(request.getResolutionNotes());

        validateStatusTransition(ticket.getStatus(), targetStatus);
        validateResolutionRequirements(ticket, targetStatus, resolutionNotes);

        ticket.setStatus(targetStatus);
        if (resolutionNotes != null) {
            ticket.setResolutionNotes(resolutionNotes);
        }

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse assignTechnician(Long id, AssignTechnicianRequest request) {
        Ticket ticket = findTicketById(id);

        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new InvalidTicketStateException("Only open or in-progress tickets can be assigned to a technician.");
        }

        ticket.setAssignedTechnician(normalizeText(request.getAssignedTechnician()));

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = findTicketById(id);

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            return;
        }

        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new InvalidTicketStateException("Only open or in-progress tickets can be cancelled.");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
    }

    private Ticket findTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
    }

    private void validateTicketCanBeUpdated(Ticket ticket) {
        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new InvalidTicketStateException("Only open or in-progress tickets can be updated.");
        }
    }

    private void validateStatusTransition(TicketStatus currentStatus, TicketStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        boolean validTransition = switch (currentStatus) {
            case OPEN -> targetStatus == TicketStatus.IN_PROGRESS
                    || targetStatus == TicketStatus.RESOLVED
                    || targetStatus == TicketStatus.CANCELLED;
            case IN_PROGRESS -> targetStatus == TicketStatus.RESOLVED
                    || targetStatus == TicketStatus.CANCELLED;
            case RESOLVED -> targetStatus == TicketStatus.CLOSED
                    || targetStatus == TicketStatus.IN_PROGRESS;
            case CLOSED, CANCELLED -> false;
        };

        if (!validTransition) {
            throw new InvalidTicketStateException(
                    "Invalid ticket status transition from " + currentStatus + " to " + targetStatus + "."
            );
        }
    }

    private void validateResolutionRequirements(Ticket ticket, TicketStatus targetStatus, String resolutionNotes) {
        if (targetStatus == TicketStatus.RESOLVED && !StringUtils.hasText(resolutionNotes)) {
            throw new InvalidTicketStateException("Resolution notes are required when resolving a ticket.");
        }

        if (targetStatus == TicketStatus.CLOSED
                && !StringUtils.hasText(resolutionNotes)
                && !StringUtils.hasText(ticket.getResolutionNotes())) {
            throw new InvalidTicketStateException("Resolution notes are required before closing a ticket.");
        }
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }

        List<String> normalizedImageUrls = imageUrls.stream()
                .map(String::trim)
                .toList();

        if (normalizedImageUrls.size() > 3) {
            throw new TooManyAttachmentsException("A maximum of 3 image URLs is allowed.");
        }

        return normalizedImageUrls;
    }

    private void applyImageUrls(Ticket ticket, List<String> imageUrls) {
        ticket.setImageUrl1(imageUrls.size() > 0 ? imageUrls.get(0) : null);
        ticket.setImageUrl2(imageUrls.size() > 1 ? imageUrls.get(1) : null);
        ticket.setImageUrl3(imageUrls.size() > 2 ? imageUrls.get(2) : null);
    }

    private TicketStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return TicketStatus.from(status);
    }

    private TicketPriority parsePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return null;
        }
        return TicketPriority.from(priority);
    }

    private TicketCategory parseCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        return TicketCategory.from(category);
    }

    private String normalizeText(String value) {
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
