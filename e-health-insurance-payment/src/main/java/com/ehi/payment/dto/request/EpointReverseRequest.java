package com.ehi.payment.dto.request;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record EpointReverseRequest(
        @Positive BigDecimal amount
) {
}
