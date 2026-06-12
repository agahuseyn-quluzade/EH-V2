package com.ehi.claim.kafka;

import com.ehi.claim.service.ClaimService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.event.FraudDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDetectedEventConsumer {

    private final ClaimService claimService;

    @KafkaListener(topics = KafkaTopics.FRAUD_DETECTED, groupId = "claim-service")
    public void consume(FraudDetectedEvent event) {
        log.info("Applying fraud detection result for claim {}: riskScore={}", event.claimId(), event.riskScore());
        claimService.applyFraudResult(event);
    }
}
