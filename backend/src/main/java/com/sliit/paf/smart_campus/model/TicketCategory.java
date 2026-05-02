package com.sliit.paf.smart_campus.model;

import java.util.Locale;

public enum TicketCategory {
    ELECTRICAL,
    PLUMBING,
    NETWORK,
    CLEANING,
    SAFETY,
    OTHER;

    public static TicketCategory from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ticket category is required.");
        }

        String normalizedValue = value.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        try {
            return TicketCategory.valueOf(normalizedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid ticket category: " + value);
        }
    }
}
