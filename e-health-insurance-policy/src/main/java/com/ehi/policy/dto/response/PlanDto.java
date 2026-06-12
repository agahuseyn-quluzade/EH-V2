package com.ehi.policy.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record PlanDto(
        UUID id,
        String name,
        String description,
        BigDecimal coverageAmount,
        BigDecimal premiumAmount,
        int durationMonths,
        boolean active
) {
}
