package com.ehi.policy;

import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PolicyStatus;
import com.ehi.infra.event.PaymentCompletedEvent;
import com.ehi.policy.entity.Plan;
import com.ehi.policy.entity.Policy;
import com.ehi.policy.repository.PlanRepository;
import com.ehi.policy.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.PAYMENT_COMPLETED})
class PolicyActivationIT {

    @Autowired PlanRepository planRepository;
    @Autowired PolicyRepository policyRepository;
    @Autowired KafkaTemplate<String, Object> kafkaTemplate;

    private Plan savePlan() {
        return planRepository.save(Plan.builder()
                .name("Basic-" + UUID.randomUUID())
                .description("Basic coverage")
                .coverageAmount(BigDecimal.valueOf(10000))
                .premiumAmount(BigDecimal.valueOf(50))
                .durationMonths(12)
                .active(true)
                .build());
    }

    private Policy savePendingPolicy(Plan plan) {
        return policyRepository.save(Policy.builder()
                .policyNumber("POL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .userId(UUID.randomUUID())
                .plan(plan)
                .status(PolicyStatus.PENDING)
                .premiumAmount(plan.getPremiumAmount())
                .build());
    }

    @Test
    void paymentCompleted_forPolicyPremium_activatesPolicy() {
        Plan plan = savePlan();
        Policy policy = savePendingPolicy(plan);

        kafkaTemplate.send(KafkaTopics.PAYMENT_COMPLETED, policy.getId().toString(),
                PaymentCompletedEvent.builder()
                        .paymentId(UUID.randomUUID())
                        .userId(policy.getUserId())
                        .referenceId(policy.getId())
                        .referenceType(PaymentReferenceType.POLICY_PREMIUM)
                        .amount(plan.getPremiumAmount())
                        .transactionId("txn-" + UUID.randomUUID())
                        .build());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Policy activated = policyRepository.findById(policy.getId()).orElseThrow();
            assertThat(activated.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
            assertThat(activated.getStartDate()).isNotNull();
            assertThat(activated.getEndDate()).isEqualTo(
                    activated.getStartDate().atZone(ZoneOffset.UTC).plusMonths(plan.getDurationMonths()).toInstant());
        });
    }

    @Test
    void paymentCompleted_forClaimPayout_isIgnored() {
        Plan plan = savePlan();
        Policy policy = savePendingPolicy(plan);

        kafkaTemplate.send(KafkaTopics.PAYMENT_COMPLETED, policy.getId().toString(),
                PaymentCompletedEvent.builder()
                        .paymentId(UUID.randomUUID())
                        .userId(policy.getUserId())
                        .referenceId(policy.getId())
                        .referenceType(PaymentReferenceType.CLAIM_PAYOUT)
                        .amount(BigDecimal.valueOf(100))
                        .transactionId("txn-" + UUID.randomUUID())
                        .build());

        // Give the consumer time to (not) act, then confirm the policy was left untouched
        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Policy unchanged = policyRepository.findById(policy.getId()).orElseThrow();
            assertThat(unchanged.getStatus()).isEqualTo(PolicyStatus.PENDING);
            assertThat(unchanged.getStartDate()).isNull();
        });
    }
}
