package com.ehi.payment.dto.request;

import com.ehi.infra.enums.PaymentReferenceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record EpointInitPaymentRequest(
        @NotNull UUID referenceId,
        PaymentReferenceType referenceType,
        @NotNull @Positive BigDecimal amount,
        String description
) {
}
