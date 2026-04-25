package com.sliit.paf.smart_campus.dto;

import com.sliit.paf.smart_campus.model.Role;
import com.sliit.paf.smart_campus.validation.ValidEnumValue;
import jakarta.validation.constraints.NotBlank;
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
public class UpdateUserRoleRequest {

    @NotBlank(message = "Role is required.")
    @ValidEnumValue(enumClass = Role.class, message = "Role must be one of: USER, ADMIN.")
    private String role;
}
