package com.sliit.paf.smart_campus.dto;

import com.sliit.paf.smart_campus.validation.AvailabilityRangeRequest;
import com.sliit.paf.smart_campus.validation.ValidEnumValue;
import com.sliit.paf.smart_campus.validation.ValidAvailabilityRange;
import com.sliit.paf.smart_campus.model.ResourceStatus;
import com.sliit.paf.smart_campus.model.ResourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidAvailabilityRange
public class UpdateResourceRequest implements AvailabilityRangeRequest {

    @NotBlank(message = "Resource code is required.")
    private String resourceCode;

    @NotBlank(message = "Resource name is required.")
    private String name;

    private String description;

    @NotBlank(message = "Resource type is required.")
    @ValidEnumValue(enumClass = ResourceType.class, message = "Resource type must be one of: LECTURE_HALL, LAB, MEETING_ROOM, EQUIPMENT.")
    private String type;

    @NotNull(message = "Capacity is required.")
    @Min(value = 1, message = "Capacity must be at least 1.")
    private Integer capacity;

    @NotBlank(message = "Location is required.")
    private String location;

    private LocalDateTime availabilityStart;

    private LocalDateTime availabilityEnd;

    @NotBlank(message = "Resource status is required.")
    @ValidEnumValue(enumClass = ResourceStatus.class, message = "Resource status must be one of: ACTIVE, OUT_OF_SERVICE.")
    private String status;
}
