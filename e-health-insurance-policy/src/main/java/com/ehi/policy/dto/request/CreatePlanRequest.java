package com.ehi.policy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreatePlanRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull @Positive BigDecimal coverageAmount,
        @NotNull @Positive BigDecimal premiumAmount,
        @Positive int durationMonths
) {
}
