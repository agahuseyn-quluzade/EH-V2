package com.eHealthInsurance.common.events.healthrecord;

import com.eHealthInsurance.common.events.DomainEvent;
import com.eHealthInsurance.common.events.EventTopics;
import com.eHealthInsurance.common.events.EventValidation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HealthRecordUpdatedEvent(
        String schemaVersion,
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        String causationId,
        String producer,
        UUID aggregateId,
        UUID userId,
        UUID healthRecordId,
        UUID memberId,
        String status,
        String changeType,
        String changedResourceType,
        UUID changedResourceId,
        Instant updatedAt
) implements DomainEvent {
    public static final String EVENT_TYPE = EventTopics.HEALTH_RECORD_UPDATED;

    public HealthRecordUpdatedEvent {
        EventValidation.requireMetadata(schemaVersion, eventId, occurredAt, correlationId, producer, aggregateId);
        EventValidation.requireNonNull(healthRecordId, "healthRecordId");
        EventValidation.requireNonNull(memberId, "memberId");
        EventValidation.requireText(status, "status");
        EventValidation.requireText(changeType, "changeType");
        EventValidation.requireNonNull(updatedAt, "updatedAt");
        if (!aggregateId.equals(healthRecordId)) {
            throw new IllegalArgumentException("aggregateId must match healthRecordId");
        }
    }

    @Override
    public String topic() {
        return EVENT_TYPE;
    }
}
