package com.eHealthInsurance.common.events;

import java.time.Instant;
import java.util.UUID;

public final class EventValidation {
    private EventValidation() {
    }

    public static void requireMetadata(
            String schemaVersion,
            UUID eventId,
            Instant occurredAt,
            String correlationId,
            String producer,
            UUID aggregateId
    ) {
        requireText(schemaVersion, "schemaVersion");
        requireNonNull(eventId, "eventId");
        requireNonNull(occurredAt, "occurredAt");
        requireText(correlationId, "correlationId");
        requireText(producer, "producer");
        requireNonNull(aggregateId, "aggregateId");
    }

    public static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }
}
