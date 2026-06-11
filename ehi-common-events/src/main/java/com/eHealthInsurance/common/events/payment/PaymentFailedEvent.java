package com.eHealthInsurance.common.events.payment;

import com.eHealthInsurance.common.events.DomainEvent;
import com.eHealthInsurance.common.events.EventTopics;
import com.eHealthInsurance.common.events.EventValidation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentFailedEvent(
        String schemaVersion,
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        String causationId,
        String producer,
        UUID aggregateId,
        UUID userId,
        UUID paymentId,
        UUID policyId,
        UUID memberId,
        BigDecimal amount,
        String currency,
        String provider,
        String failureCode,
        String failureReason
) implements DomainEvent {
    public static final String EVENT_TYPE = EventTopics.PAYMENT_FAILED;

    public PaymentFailedEvent {
        EventValidation.requireMetadata(schemaVersion, eventId, occurredAt, correlationId, producer, aggregateId);
        EventValidation.requireNonNull(paymentId, "paymentId");
        EventValidation.requireNonNull(policyId, "policyId");
        EventValidation.requireNonNull(memberId, "memberId");
        EventValidation.requireNonNull(amount, "amount");
        EventValidation.requireText(currency, "currency");
        EventValidation.requireText(provider, "provider");
        EventValidation.requireText(failureReason, "failureReason");
        if (!aggregateId.equals(paymentId)) {
            throw new IllegalArgumentException("aggregateId must match paymentId");
        }
    }

    @Override
    public String topic() {
        return EVENT_TYPE;
    }
}
