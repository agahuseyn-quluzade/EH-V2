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
public record PolicyPurchaseRequestedEvent(
        String schemaVersion,
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        String causationId,
        String producer,
        UUID aggregateId,
        UUID userId,
        UUID purchaseRequestId,
        UUID memberId,
        UUID planId,
        LocalDate requestedStartDate,
        BigDecimal quotedMonthlyPremium,
        String currency
) implements DomainEvent {
    public static final String EVENT_TYPE = EventTopics.POLICY_PURCHASE_REQUESTED;

    public PolicyPurchaseRequestedEvent {
        EventValidation.requireMetadata(schemaVersion, eventId, occurredAt, correlationId, producer, aggregateId);
        EventValidation.requireNonNull(purchaseRequestId, "purchaseRequestId");
        EventValidation.requireNonNull(memberId, "memberId");
        EventValidation.requireNonNull(planId, "planId");
        EventValidation.requireNonNull(requestedStartDate, "requestedStartDate");
        if (!aggregateId.equals(purchaseRequestId)) {
            throw new IllegalArgumentException("aggregateId must match purchaseRequestId");
        }
    }

    @Override
    public String topic() {
        return EVENT_TYPE;
    }
}
