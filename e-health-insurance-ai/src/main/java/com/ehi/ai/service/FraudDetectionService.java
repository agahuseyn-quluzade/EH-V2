package com.ehi.ai.service;

import com.ehi.ai.dto.response.FraudAiResponse;
import com.ehi.infra.event.ClaimSubmittedEvent;

import java.util.UUID;

public interface FraudDetectionService {

    FraudAiResponse evaluateClaim(ClaimSubmittedEvent event);

    FraudAiResponse getFraudCheck(UUID claimId);

    FraudAiResponse reanalyzeClaim(UUID claimId);
}
