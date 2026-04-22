package com.sliit.paf.smart_campus.dto;

import com.sliit.paf.smart_campus.model.BookingStatus;
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
public class UpdateBookingStatusRequest {

    @NotBlank(message = "Booking status is required.")
    @ValidEnumValue(
            enumClass = BookingStatus.class,
            message = "Booking status must be one of: PENDING, APPROVED, REJECTED, CANCELLED."
    )
    private String status;

    private String adminDecisionReason;
}
