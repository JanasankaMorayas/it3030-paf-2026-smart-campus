package com.sliit.paf.smart_campus.model;

import java.util.Locale;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    public static TicketStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ticket status is required.");
        }

        String normalizedValue = value.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        try {
            return TicketStatus.valueOf(normalizedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid ticket status: " + value);
        }
    }
}
