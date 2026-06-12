package com.ehi.ai.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record RiskAiResponse(
        UUID userId,
        int totalClaims,
        double averageRiskScore,
        int highRiskCount,
        Instant lastClaimAt
) {
}
