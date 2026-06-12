package com.ehi.ai.kafka;

import com.ehi.ai.service.FraudDetectionService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.event.ClaimSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimSubmittedEventConsumer {

    private final FraudDetectionService fraudDetectionService;

    @KafkaListener(topics = KafkaTopics.CLAIM_SUBMITTED, groupId = "ai-service")
    public void consume(ClaimSubmittedEvent event) {
        log.info("Received ClaimSubmittedEvent for claimId={}, userId={}", event.claimId(), event.userId());
        fraudDetectionService.evaluateClaim(event);
    }
}
