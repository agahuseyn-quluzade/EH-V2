package com.ehi.ai.kafka;

import com.ehi.ai.entity.PolicyCoverage;
import com.ehi.ai.repository.PolicyCoverageRepository;
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

    private final PolicyCoverageRepository policyCoverageRepository;

    @KafkaListener(topics = KafkaTopics.POLICY_CREATED, groupId = "ai-service")
    public void consume(PolicyCreatedEvent event) {
        log.info("Storing coverage for policyId={}, coverage={}", event.policyId(), event.coverageAmount());

        policyCoverageRepository.findByPolicyId(event.policyId()).ifPresentOrElse(
                existing -> {
                    existing.setCoverageAmount(event.coverageAmount());
                    policyCoverageRepository.save(existing);
                },
                () -> policyCoverageRepository.save(PolicyCoverage.builder()
                        .policyId(event.policyId())
                        .coverageAmount(event.coverageAmount())
                        .build()));
    }
}
