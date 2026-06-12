package com.ehi.infra.event;

import com.ehi.infra.enums.PaymentReferenceType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record PaymentCompletedEvent(
        UUID paymentId,
        UUID userId,
        UUID referenceId,
        PaymentReferenceType referenceType,
        BigDecimal amount,
        String transactionId
) {
}
