package com.sliit.paf.smart_campus.model;

import java.util.Locale;

public enum ResourceStatus {
    ACTIVE,
    OUT_OF_SERVICE;

    public static ResourceStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Resource status is required.");
        }

        String normalizedValue = value.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        try {
            return ResourceStatus.valueOf(normalizedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid resource status: " + value);
        }
    }
}
