package com.ehi.payment.dto.response;

import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentDto(
        UUID id,
        UUID userId,
        UUID referenceId,
        PaymentReferenceType referenceType,
        BigDecimal amount,
        PaymentStatus status,
        String transactionId,
        String failureReason,
        Instant createdAt
) {
}
