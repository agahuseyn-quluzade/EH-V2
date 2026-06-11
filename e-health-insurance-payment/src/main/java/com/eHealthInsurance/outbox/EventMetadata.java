package com.eHealthInsurance.outbox;

import java.time.Instant;
import java.util.UUID;

public record EventMetadata(
        String schemaVersion,
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        String causationId,
        String producer,
        UUID aggregateId,
        UUID userId
) {
}
