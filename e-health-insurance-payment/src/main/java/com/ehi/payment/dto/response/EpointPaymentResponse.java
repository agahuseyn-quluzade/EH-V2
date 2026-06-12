package com.ehi.payment.dto.response;

import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record EpointPaymentResponse(
        UUID id,
        UUID paymentId,
        UUID userId,
        UUID referenceId,
        PaymentReferenceType referenceType,
        BigDecimal amount,
        String currency,
        String orderId,
        PaymentStatus status,
        String redirectUrl,
        String epointTransaction,
        String bankTransaction,
        String cardMask,
        String cardName,
        String gatewayStatus,
        String gatewayCode,
        String operationCode,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
