package com.ehi.infra.event;

import com.ehi.infra.enums.ClaimStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ClaimDecisionEvent(
        UUID claimId,
        UUID userId,
        UUID policyId,
        ClaimStatus decision,
        BigDecimal approvedAmount,
        String rejectionReason,
        UUID reviewedBy
) {
}
