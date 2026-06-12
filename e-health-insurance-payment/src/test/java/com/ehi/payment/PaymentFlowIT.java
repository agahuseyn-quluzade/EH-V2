package com.ehi.payment;

import com.ehi.payment.entity.Payment;
import com.ehi.payment.repository.PaymentRepository;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.ClaimStatus;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import com.ehi.infra.event.ClaimDecisionEvent;
import com.ehi.infra.event.PolicyCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopics.POLICY_CREATED, KafkaTopics.CLAIM_DECISION, KafkaTopics.PAYMENT_COMPLETED},
        brokerProperties = {
                "offsets.topic.num.partitions=1",
                "log.cleaner.enable=false",
                "controlled.shutdown.enable=false"
        }
)
class PaymentFlowIT {

    @Autowired PaymentRepository paymentRepository;
    @Autowired KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void policyCreated_doesNotCompletePremiumPayment_withoutEpointResult() {
        UUID policyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        kafkaTemplate.send(KafkaTopics.POLICY_CREATED, policyId.toString(),
                PolicyCreatedEvent.builder()
                        .policyId(policyId).userId(userId).planId(UUID.randomUUID())
                        .policyNumber("POL-1").premiumAmount(BigDecimal.valueOf(150)).build());

        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(paymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(
                        policyId, PaymentReferenceType.POLICY_PREMIUM, PaymentStatus.FAILED)).isEmpty());
    }

    @Test
    void claimDecisionApproved_createsPayoutPayment() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        kafkaTemplate.send(KafkaTopics.CLAIM_DECISION, claimId.toString(),
                ClaimDecisionEvent.builder()
                        .claimId(claimId).userId(userId).policyId(UUID.randomUUID())
                        .decision(ClaimStatus.APPROVED).approvedAmount(BigDecimal.valueOf(300))
                        .rejectionReason(null).reviewedBy(UUID.randomUUID()).build());

        Payment payment = awaitCompletedPayment(claimId, PaymentReferenceType.CLAIM_PAYOUT);
        assertThat(payment.getUserId()).isEqualTo(userId);
        assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    void claimDecisionRejected_doesNotCreatePayment() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        kafkaTemplate.send(KafkaTopics.CLAIM_DECISION, claimId.toString(),
                ClaimDecisionEvent.builder()
                        .claimId(claimId).userId(userId).policyId(UUID.randomUUID())
                        .decision(ClaimStatus.REJECTED).approvedAmount(BigDecimal.ZERO)
                        .rejectionReason("High fraud risk").reviewedBy(UUID.randomUUID()).build());

        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(paymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(
                        claimId, PaymentReferenceType.CLAIM_PAYOUT, PaymentStatus.FAILED)).isEmpty());
    }

    private Payment awaitCompletedPayment(UUID referenceId, PaymentReferenceType referenceType) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<Payment> found = paymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(
                    referenceId, referenceType, PaymentStatus.FAILED);
            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        });
        return paymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(
                referenceId, referenceType, PaymentStatus.FAILED).orElseThrow();
    }
}
