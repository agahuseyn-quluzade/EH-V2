package com.ehi.claim.dto.response;

import com.ehi.infra.enums.ClaimStatus;
import com.ehi.infra.enums.ClaimType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record ClaimDto(
        UUID id,
        String claimNumber,
        UUID userId,
        UUID policyId,
        ClaimType claimType,
        BigDecimal amount,
        String description,
        ClaimStatus status,
        BigDecimal approvedAmount,
        String rejectionReason,
        UUID reviewedBy,
        Integer riskScore,
        List<String> fraudFlags,
        String aiExplanation,
        Instant createdAt
) {
}
