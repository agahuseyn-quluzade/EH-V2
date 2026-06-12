package com.ehi.payment.kafka;

import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.event.PolicyCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyCreatedEventConsumer {

    @KafkaListener(topics = KafkaTopics.POLICY_CREATED, groupId = "payment-service")
    public void consume(PolicyCreatedEvent event) {
        log.info("Received PolicyCreatedEvent for policyId={}, waiting for Epoint init", event.policyId());
    }
}
