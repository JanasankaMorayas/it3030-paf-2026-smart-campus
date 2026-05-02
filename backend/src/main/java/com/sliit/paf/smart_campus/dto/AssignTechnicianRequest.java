package com.sliit.paf.smart_campus.dto;

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
public class AssignTechnicianRequest {

    @NotBlank(message = "Assigned technician is required.")
    @Size(max = 100, message = "Assigned technician must not exceed 100 characters.")
    private String assignedTechnician;
}
