package com.eHealthInsurance.common.events.policy;

import com.eHealthInsurance.common.events.DomainEvent;
import com.eHealthInsurance.common.events.EventTopics;
import com.eHealthInsurance.common.events.EventValidation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyCancelledEvent(
        String schemaVersion,
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        String causationId,
        String producer,
        UUID aggregateId,
        UUID userId,
        UUID policyId,
        UUID memberId,
        String cancellationReason,
        Instant cancelledAt,
        String previousStatus,
        String status
) implements DomainEvent {
    public static final String EVENT_TYPE = EventTopics.POLICY_CANCELLED;

    public PolicyCancelledEvent {
        EventValidation.requireMetadata(schemaVersion, eventId, occurredAt, correlationId, producer, aggregateId);
        EventValidation.requireNonNull(policyId, "policyId");
        EventValidation.requireNonNull(memberId, "memberId");
        EventValidation.requireText(cancellationReason, "cancellationReason");
        EventValidation.requireNonNull(cancelledAt, "cancelledAt");
        EventValidation.requireText(status, "status");
        if (!aggregateId.equals(policyId)) {
            throw new IllegalArgumentException("aggregateId must match policyId");
        }
    }

    @Override
    public String topic() {
        return EVENT_TYPE;
    }
}
