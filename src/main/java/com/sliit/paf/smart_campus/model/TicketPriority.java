package com.sliit.paf.smart_campus.model;

import java.util.Locale;

public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static TicketPriority from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ticket priority is required.");
        }

        String normalizedValue = value.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        try {
            return TicketPriority.valueOf(normalizedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid ticket priority: " + value);
        }
    }
}
