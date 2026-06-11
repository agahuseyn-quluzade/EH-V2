package com.eHealthInsurance.common.events.audit;

import com.eHealthInsurance.common.events.DomainEvent;
import com.eHealthInsurance.common.events.EventTopics;
import com.eHealthInsurance.common.events.EventValidation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuditEventCreatedEvent(
        String schemaVersion,
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        String causationId,
        String producer,
        UUID aggregateId,
        UUID userId,
        String action,
        String resourceType,
        UUID resourceId,
        String outcome,
        Map<String, Object> attributes
) implements DomainEvent {
    public static final String EVENT_TYPE = EventTopics.AUDIT_EVENT_CREATED;

    public AuditEventCreatedEvent {
        EventValidation.requireMetadata(schemaVersion, eventId, occurredAt, correlationId, producer, aggregateId);
        EventValidation.requireText(action, "action");
        EventValidation.requireText(resourceType, "resourceType");
        EventValidation.requireText(outcome, "outcome");
    }

    @Override
    public String topic() {
        return EVENT_TYPE;
    }
}
