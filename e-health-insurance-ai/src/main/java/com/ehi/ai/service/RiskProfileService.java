package com.ehi.ai.service;

import com.ehi.ai.dto.response.RiskAiResponse;

import java.util.UUID;

public interface RiskProfileService {

    RiskAiResponse getRiskProfile(UUID userId);

    void recordFraudCheck(UUID userId, int riskScore, boolean highRisk);

    boolean isHighRiskUser(UUID userId);
}
