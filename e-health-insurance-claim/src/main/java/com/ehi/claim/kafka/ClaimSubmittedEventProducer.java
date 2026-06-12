package com.ehi.claim.kafka;

import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.event.ClaimSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimSubmittedEventProducer {

    private final KafkaTemplate<String, ClaimSubmittedEvent> kafkaTemplate;

    public void publish(ClaimSubmittedEvent event) {
        kafkaTemplate.send(KafkaTopics.CLAIM_SUBMITTED, event.claimId().toString(), event);
        log.info("Published ClaimSubmittedEvent for claimId={}", event.claimId());
    }
}
