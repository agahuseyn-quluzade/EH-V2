package com.ehi.policy.kafka;

import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.event.PolicyCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyCreatedEventProducer {

    private final KafkaTemplate<String, PolicyCreatedEvent> kafkaTemplate;

    public void publish(PolicyCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.POLICY_CREATED, event.policyId().toString(), event);
        log.info("Published PolicyCreatedEvent for policyId={}", event.policyId());
    }
}
