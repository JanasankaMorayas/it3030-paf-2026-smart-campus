package com.sliit.paf.smart_campus.dto;

import com.sliit.paf.smart_campus.validation.BookingTimeRangeRequest;
import com.sliit.paf.smart_campus.validation.ValidBookingTimeRange;
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
@ValidBookingTimeRange
public class CreateBookingRequest implements BookingTimeRangeRequest {

    @NotNull(message = "Resource id is required.")
    @Min(value = 1, message = "Resource id must be at least 1.")
    private Long resourceId;

    @NotBlank(message = "Requester id is required.")
    private String requesterId;

    @NotBlank(message = "Purpose is required.")
    private String purpose;

    @NotNull(message = "Expected attendees is required.")
    @Min(value = 1, message = "Expected attendees must be at least 1.")
    private Integer expectedAttendees;

    @NotNull(message = "Start time is required.")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required.")
    private LocalDateTime endTime;
}
