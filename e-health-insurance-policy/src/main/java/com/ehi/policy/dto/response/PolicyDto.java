package com.ehi.policy.dto.response;

import com.ehi.infra.enums.PolicyStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PolicyDto(
        UUID id,
        String policyNumber,
        UUID userId,
        UUID planId,
        String planName,
        PolicyStatus status,
        BigDecimal premiumAmount,
        Instant startDate,
        Instant endDate
) {
}
