package com.sliit.paf.smart_campus.model;

import java.util.Locale;

public enum BookingStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    public static BookingStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Booking status is required.");
        }

        String normalizedValue = value.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        try {
            return BookingStatus.valueOf(normalizedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid booking status: " + value);
        }
    }
}
