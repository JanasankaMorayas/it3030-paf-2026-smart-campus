package com.sliit.paf.smart_campus.dto;

import com.sliit.paf.smart_campus.model.Ticket;
import com.sliit.paf.smart_campus.model.TicketCategory;
import com.sliit.paf.smart_campus.model.TicketPriority;
import com.sliit.paf.smart_campus.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private Long id;
    private String title;
    private String description;
    private TicketCategory category;
    private TicketPriority priority;
    private TicketStatus status;
    private String location;
    private Long reportedByUserId;
    private String reporterEmail;
    private String reporterDisplayName;
    private String reportedBy;
    private Long assignedTechnicianUserId;
    private String technicianEmail;
    private String technicianDisplayName;
    private String assignedTechnician;
    private String resolutionNotes;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketResponse from(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .location(ticket.getLocation())
                .reportedByUserId(ticket.getReportedByUser() != null ? ticket.getReportedByUser().getId() : null)
                .reporterEmail(ticket.getReportedByUser() != null ? ticket.getReportedByUser().getEmail() : ticket.getReportedBy())
                .reporterDisplayName(ticket.getReportedByUser() != null ? ticket.getReportedByUser().getDisplayName() : null)
                .reportedBy(ticket.getReportedByUser() != null ? ticket.getReportedByUser().getEmail() : ticket.getReportedBy())
                .assignedTechnicianUserId(ticket.getAssignedTechnicianUser() != null ? ticket.getAssignedTechnicianUser().getId() : null)
                .technicianEmail(ticket.getAssignedTechnicianUser() != null ? ticket.getAssignedTechnicianUser().getEmail() : ticket.getAssignedTechnician())
                .technicianDisplayName(ticket.getAssignedTechnicianUser() != null ? ticket.getAssignedTechnicianUser().getDisplayName() : null)
                .assignedTechnician(ticket.getAssignedTechnicianUser() != null ? ticket.getAssignedTechnicianUser().getEmail() : ticket.getAssignedTechnician())
                .resolutionNotes(ticket.getResolutionNotes())
                .imageUrls(
                        Stream.of(ticket.getImageUrl1(), ticket.getImageUrl2(), ticket.getImageUrl3())
                                .filter(value -> value != null && !value.isBlank())
                                .toList()
                )
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
