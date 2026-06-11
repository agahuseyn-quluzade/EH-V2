package com.eHealthInsurance.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateRefundRequest(
    @NotNull UUID paymentId,
    @NotNull BigDecimal amount,
    String reason
) {}
