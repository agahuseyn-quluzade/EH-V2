package com.eHealthInsurance.common.events.policy;

import com.eHealthInsurance.common.events.DomainEvent;
import com.eHealthInsurance.common.events.EventTopics;
import com.eHealthInsurance.common.events.EventValidation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyActivatedEvent(
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
        UUID planId,
        Long planVersion,
        String planName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyPremium,
        String currency,
        String status
) implements DomainEvent {
    public static final String EVENT_TYPE = EventTopics.POLICY_ACTIVATED;

    public PolicyActivatedEvent {
        EventValidation.requireMetadata(schemaVersion, eventId, occurredAt, correlationId, producer, aggregateId);
        EventValidation.requireNonNull(policyId, "policyId");
        EventValidation.requireNonNull(memberId, "memberId");
        EventValidation.requireNonNull(planId, "planId");
        EventValidation.requireNonNull(startDate, "startDate");
        EventValidation.requireNonNull(endDate, "endDate");
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
