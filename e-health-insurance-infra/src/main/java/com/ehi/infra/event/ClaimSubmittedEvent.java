package com.ehi.infra.event;

import com.ehi.infra.enums.ClaimType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ClaimSubmittedEvent(
        UUID claimId,
        UUID userId,
        UUID policyId,
        String claimNumber,
        ClaimType claimType,
        BigDecimal amount
) {
}
