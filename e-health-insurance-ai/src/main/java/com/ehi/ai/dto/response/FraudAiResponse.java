package com.ehi.ai.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record FraudAiResponse(
        UUID claimId,
        UUID userId,
        Integer ruleScore,
        Integer aiScore,
        Integer finalScore,
        List<String> flags,
        String aiExplanation,
        Instant createdAt
) {
}
