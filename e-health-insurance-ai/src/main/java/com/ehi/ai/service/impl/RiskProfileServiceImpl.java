package com.ehi.ai.service.impl;

import com.ehi.ai.dto.response.RiskAiResponse;
import com.ehi.ai.entity.RiskProfile;
import com.ehi.ai.mapper.RiskProfileMapper;
import com.ehi.ai.repository.RiskProfileRepository;
import com.ehi.ai.service.RiskProfileService;
import com.ehi.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiskProfileServiceImpl implements RiskProfileService {

    private static final int HIGH_RISK_USER_THRESHOLD = 2;

    private final RiskProfileRepository riskProfileRepository;
    private final RiskProfileMapper riskProfileMapper;

    @Override
    public RiskAiResponse getRiskProfile(UUID userId) {
        return riskProfileMapper.toDto(riskProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("RiskProfile", userId)));
    }

    @Override
    public void recordFraudCheck(UUID userId, int riskScore, boolean highRisk) {
        RiskProfile profile = riskProfileRepository.findByUserId(userId)
                .orElseGet(() -> RiskProfile.builder()
                        .userId(userId)
                        .totalClaims(0)
                        .averageRiskScore(0.0)
                        .highRiskCount(0)
                        .build());

        int newTotal = profile.getTotalClaims() + 1;
        double newAverage = (profile.getAverageRiskScore() * profile.getTotalClaims() + riskScore) / newTotal;

        profile.setTotalClaims(newTotal);
        profile.setAverageRiskScore(newAverage);
        if (highRisk) {
            profile.setHighRiskCount(profile.getHighRiskCount() + 1);
        }
        profile.setLastClaimAt(Instant.now());

        riskProfileRepository.save(profile);
    }

    @Override
    public boolean isHighRiskUser(UUID userId) {
        return riskProfileRepository.findByUserId(userId)
                .map(profile -> profile.getHighRiskCount() >= HIGH_RISK_USER_THRESHOLD)
                .orElse(false);
    }
}
