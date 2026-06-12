package com.ehi.claim.kafka;

import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.event.ClaimDecisionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimDecisionEventProducer {

    private final KafkaTemplate<String, ClaimDecisionEvent> kafkaTemplate;

    public void publish(ClaimDecisionEvent event) {
        kafkaTemplate.send(KafkaTopics.CLAIM_DECISION, event.claimId().toString(), event);
        log.info("Published ClaimDecisionEvent for claimId={}", event.claimId());
    }
}
