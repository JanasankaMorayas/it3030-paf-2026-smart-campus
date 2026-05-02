package com.sliit.paf.smart_campus.model;

import java.util.Locale;

public enum ResourceType {
    LECTURE_HALL,
    LAB,
    MEETING_ROOM,
    EQUIPMENT;

    public static ResourceType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Resource type is required.");
        }

        String normalizedValue = value.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        try {
            return ResourceType.valueOf(normalizedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid resource type: " + value);
        }
    }
}
