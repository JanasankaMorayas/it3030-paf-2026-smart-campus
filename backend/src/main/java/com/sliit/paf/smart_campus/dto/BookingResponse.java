package com.sliit.paf.smart_campus.dto;

import com.sliit.paf.smart_campus.model.Booking;
import com.sliit.paf.smart_campus.model.BookingStatus;
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
public class BookingResponse {

    private Long id;
    private Long resourceId;
    private String resourceCode;
    private String resourceName;
    private Long ownerUserId;
    private String ownerEmail;
    private String ownerDisplayName;
    private String requesterId;
    private String purpose;
    private Integer expectedAttendees;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private String adminDecisionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookingResponse from(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .resourceId(booking.getResource().getId())
                .resourceCode(booking.getResource().getResourceCode())
                .resourceName(booking.getResource().getName())
                .ownerUserId(booking.getOwnerUser() != null ? booking.getOwnerUser().getId() : null)
                .ownerEmail(booking.getOwnerUser() != null ? booking.getOwnerUser().getEmail() : booking.getRequesterId())
                .ownerDisplayName(booking.getOwnerUser() != null ? booking.getOwnerUser().getDisplayName() : null)
                .requesterId(booking.getOwnerUser() != null ? booking.getOwnerUser().getEmail() : booking.getRequesterId())
                .purpose(booking.getPurpose())
                .expectedAttendees(booking.getExpectedAttendees())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .adminDecisionReason(booking.getAdminDecisionReason())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
