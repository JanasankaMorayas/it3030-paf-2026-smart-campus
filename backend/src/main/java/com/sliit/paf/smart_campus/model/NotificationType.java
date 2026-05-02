package com.sliit.paf.smart_campus.model;

import java.util.Arrays;

public enum NotificationType {
    BOOKING_CREATED,
    BOOKING_APPROVED,
    BOOKING_REJECTED,
    BOOKING_CANCELLED,
    TICKET_CREATED,
    TICKET_ASSIGNED,
    TICKET_STATUS_UPDATED,
    TICKET_RESOLVED,
    GENERAL;

    public static NotificationType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notification type must be one of: "
                                + Arrays.stream(values()).map(Enum::name).reduce((left, right) -> left + ", " + right).orElse("")
                                + "."
                ));
    }
}
