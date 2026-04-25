package com.sliit.paf.smart_campus.dto;

import com.sliit.paf.smart_campus.model.TicketCategory;
import com.sliit.paf.smart_campus.model.TicketPriority;
import com.sliit.paf.smart_campus.validation.MaxTicketAttachments;
import com.sliit.paf.smart_campus.validation.ValidEnumValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotBlank(message = "Title is required.")
    @Size(max = 150, message = "Title must not exceed 150 characters.")
    private String title;

    @NotBlank(message = "Description is required.")
    @Size(max = 4000, message = "Description must not exceed 4000 characters.")
    private String description;

    @NotBlank(message = "Category is required.")
    @ValidEnumValue(
            enumClass = TicketCategory.class,
            message = "Ticket category must be one of: ELECTRICAL, PLUMBING, NETWORK, CLEANING, SAFETY, OTHER."
    )
    private String category;

    @NotBlank(message = "Priority is required.")
    @ValidEnumValue(
            enumClass = TicketPriority.class,
            message = "Ticket priority must be one of: LOW, MEDIUM, HIGH, CRITICAL."
    )
    private String priority;

    @NotBlank(message = "Location is required.")
    @Size(max = 255, message = "Location must not exceed 255 characters.")
    private String location;

    @NotBlank(message = "Reported by is required.")
    @Size(max = 100, message = "Reported by must not exceed 100 characters.")
    private String reportedBy;

    @MaxTicketAttachments
    private List<@NotBlank(message = "Image URL cannot be blank.") @Size(max = 500, message = "Image URL must not exceed 500 characters.") String> imageUrls;
}
