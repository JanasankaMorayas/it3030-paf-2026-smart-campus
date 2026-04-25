package com.sliit.paf.smart_campus.dto;

import com.sliit.paf.smart_campus.model.TicketStatus;
import com.sliit.paf.smart_campus.validation.ValidEnumValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketStatusRequest {

    @NotBlank(message = "Ticket status is required.")
    @ValidEnumValue(
            enumClass = TicketStatus.class,
            message = "Ticket status must be one of: OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED."
    )
    private String status;

    @Size(max = 4000, message = "Resolution notes must not exceed 4000 characters.")
    private String resolutionNotes;
}
