package com.ehi.claim;

import com.ehi.claim.dto.request.SubmitClaimRequest;
import com.ehi.claim.dto.response.ClaimDto;
import com.ehi.claim.entity.Claim;
import com.ehi.claim.repository.ClaimRepository;
import com.ehi.claim.service.ClaimService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.ClaimStatus;
import com.ehi.infra.enums.ClaimType;
import com.ehi.infra.event.ClaimDecisionEvent;
import com.ehi.infra.event.ClaimSubmittedEvent;
import com.ehi.infra.event.FraudDetectedEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.CLAIM_SUBMITTED, KafkaTopics.CLAIM_DECISION, KafkaTopics.FRAUD_DETECTED})
class ClaimFlowIT {

    @Autowired ClaimService claimService;
    @Autowired ClaimRepository claimRepository;
    @Autowired KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, Object> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("claim-flow-it-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new JsonDeserializer<>(Object.class).trustedPackages("*"));
        consumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromEmbeddedTopics(consumer, KafkaTopics.CLAIM_SUBMITTED, KafkaTopics.CLAIM_DECISION);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    private ClaimDto submitSampleClaim() {
        UUID userId = UUID.randomUUID();
        var request = new SubmitClaimRequest(UUID.randomUUID(), ClaimType.HOSPITALIZATION, BigDecimal.valueOf(500), "Surgery");
        return claimService.submitClaim(userId, request);
    }

    private ConsumerRecord<String, Object> awaitRecord(String topic, UUID claimId) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, Object> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2));
            for (ConsumerRecord<String, Object> record : records.records(topic)) {
                if (record.key().equals(claimId.toString())) {
                    return record;
                }
            }
        }
        throw new AssertionError("No record on topic " + topic + " for claimId " + claimId);
    }

    @Test
    void submitClaim_publishesClaimSubmittedEvent() {
        ClaimDto dto = submitSampleClaim();

        ConsumerRecord<String, Object> record = awaitRecord(KafkaTopics.CLAIM_SUBMITTED, dto.id());
        ClaimSubmittedEvent event = (ClaimSubmittedEvent) record.value();

        assertThat(event.claimId()).isEqualTo(dto.id());
        assertThat(event.userId()).isEqualTo(dto.userId());
        assertThat(event.amount()).isEqualTo(BigDecimal.valueOf(500));
    }

    @Test
    void fraudDetected_lowScore_autoApprovesAndPublishesDecision() {
        ClaimDto dto = submitSampleClaim();

        kafkaTemplate.send(KafkaTopics.FRAUD_DETECTED, dto.id().toString(),
                FraudDetectedEvent.builder()
                        .claimId(dto.id()).userId(dto.userId())
                        .riskScore(20).flags(List.of()).aiExplanation("low risk").build());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Claim claim = claimRepository.findById(dto.id()).orElseThrow();
            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
            assertThat(claim.getApprovedAmount()).isEqualTo(claim.getAmount());
        });

        ConsumerRecord<String, Object> record = awaitRecord(KafkaTopics.CLAIM_DECISION, dto.id());
        ClaimDecisionEvent event = (ClaimDecisionEvent) record.value();
        assertThat(event.decision()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(event.reviewedBy()).isNull();
    }

    @Test
    void fraudDetected_highScore_autoRejectsAndPublishesDecision() {
        ClaimDto dto = submitSampleClaim();

        kafkaTemplate.send(KafkaTopics.FRAUD_DETECTED, dto.id().toString(),
                FraudDetectedEvent.builder()
                        .claimId(dto.id()).userId(dto.userId())
                        .riskScore(85).flags(List.of("duplicate_claim")).aiExplanation("high risk").build());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Claim claim = claimRepository.findById(dto.id()).orElseThrow();
            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.REJECTED);
            assertThat(claim.getRejectionReason()).isNotBlank();
        });

        ConsumerRecord<String, Object> record = awaitRecord(KafkaTopics.CLAIM_DECISION, dto.id());
        ClaimDecisionEvent event = (ClaimDecisionEvent) record.value();
        assertThat(event.decision()).isEqualTo(ClaimStatus.REJECTED);
    }

    @Test
    void fraudDetected_midScore_setsUnderReview_withoutDecisionEvent() {
        ClaimDto dto = submitSampleClaim();

        kafkaTemplate.send(KafkaTopics.FRAUD_DETECTED, dto.id().toString(),
                FraudDetectedEvent.builder()
                        .claimId(dto.id()).userId(dto.userId())
                        .riskScore(55).flags(List.of("unusual_amount")).aiExplanation("medium risk").build());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Claim claim = claimRepository.findById(dto.id()).orElseThrow();
            assertThat(claim.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        });

        ConsumerRecords<String, Object> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2));
        boolean decisionPublished = false;
        for (ConsumerRecord<String, Object> record : records.records(KafkaTopics.CLAIM_DECISION)) {
            if (record.key().equals(dto.id().toString())) {
                decisionPublished = true;
            }
        }
        assertThat(decisionPublished).isFalse();
    }
}
