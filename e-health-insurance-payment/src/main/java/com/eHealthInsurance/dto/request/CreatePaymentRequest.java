package com.eHealthInsurance.dto.request;

import com.eHealthInsurance.entity.enums.PaymentProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
    @NotNull UUID policyId,
    @NotNull @Positive BigDecimal amount,
    @NotNull PaymentProvider provider,
    String idempotencyKey,
    String paymentMethodToken
) {}
