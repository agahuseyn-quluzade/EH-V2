package com.ehi.infra.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record PolicyCreatedEvent(
        UUID policyId,
        UUID userId,
        UUID planId,
        String policyNumber,
        BigDecimal premiumAmount,
        BigDecimal coverageAmount
) {
}
