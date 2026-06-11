package com.eHealthInsurance.common.events.claim;

import com.eHealthInsurance.common.events.DomainEvent;
import com.eHealthInsurance.common.events.EventTopics;
import com.eHealthInsurance.common.events.EventValidation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaimApprovedEvent(
        String schemaVersion,
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        String causationId,
        String producer,
        UUID aggregateId,
        UUID userId,
        UUID claimId,
        UUID memberId,
        UUID policyId,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        String currency,
        String reason,
        UUID decidedBy,
        Instant decidedAt,
        String status
) implements DomainEvent {
    public static final String EVENT_TYPE = EventTopics.CLAIM_APPROVED;

    public ClaimApprovedEvent {
        EventValidation.requireMetadata(schemaVersion, eventId, occurredAt, correlationId, producer, aggregateId);
        EventValidation.requireNonNull(claimId, "claimId");
        EventValidation.requireNonNull(memberId, "memberId");
        EventValidation.requireNonNull(policyId, "policyId");
        EventValidation.requireNonNull(requestedAmount, "requestedAmount");
        EventValidation.requireNonNull(approvedAmount, "approvedAmount");
        EventValidation.requireText(currency, "currency");
        EventValidation.requireNonNull(decidedAt, "decidedAt");
        EventValidation.requireText(status, "status");
        if (!aggregateId.equals(claimId)) {
            throw new IllegalArgumentException("aggregateId must match claimId");
        }
    }

    @Override
    public String topic() {
        return EVENT_TYPE;
    }
}
