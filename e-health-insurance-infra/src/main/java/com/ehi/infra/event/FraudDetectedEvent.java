package com.ehi.infra.event;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record FraudDetectedEvent(
        UUID claimId,
        UUID userId,
        Integer riskScore,
        List<String> flags,
        String aiExplanation
) {
}
