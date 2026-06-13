package com.ehi.ai;

import com.ehi.ai.entity.FraudCheck;
import com.ehi.ai.repository.FraudCheckRepository;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.ClaimType;
import com.ehi.infra.event.ClaimSubmittedEvent;
import com.ehi.infra.event.FraudDetectedEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
@EmbeddedKafka(partitions = 1, topics = {KafkaTopics.CLAIM_SUBMITTED, KafkaTopics.FRAUD_DETECTED})
class AiFlowIT {

    @Autowired KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired FraudCheckRepository fraudCheckRepository;

    private Consumer<String, Object> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("ai-flow-it-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new JsonDeserializer<>(Object.class).trustedPackages("*"));
        consumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromEmbeddedTopics(consumer, KafkaTopics.FRAUD_DETECTED);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    private ConsumerRecord<String, Object> awaitRecord(String topic, UUID claimId) {
        long deadline = System.currentTimeMillis() + 20_000;
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
    void claimSubmitted_belowThreshold_createsFraudCheck_withZeroScore() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        kafkaTemplate.send(KafkaTopics.CLAIM_SUBMITTED, claimId.toString(),
                ClaimSubmittedEvent.builder()
                        .claimId(claimId).userId(userId).policyId(UUID.randomUUID())
                        .claimNumber("CLM-1").claimType(ClaimType.CONSULTATION).amount(BigDecimal.valueOf(100))
                        .build());

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<FraudCheck> found = fraudCheckRepository.findByClaimId(claimId);
            assertThat(found).isPresent();
            FraudCheck fraudCheck = found.get();
            assertThat(fraudCheck.getRuleScore()).isEqualTo(0);
            assertThat(fraudCheck.getAiScore()).isNull();
            assertThat(fraudCheck.getFinalScore()).isEqualTo(0);
        });

        ConsumerRecord<String, Object> record = awaitRecord(KafkaTopics.FRAUD_DETECTED, claimId);
        FraudDetectedEvent event = (FraudDetectedEvent) record.value();
        assertThat(event.riskScore()).isEqualTo(0);
    }

    @Test
    void claimSubmitted_aboveThreshold_fallsBackToRuleOnlyScore_whenAiUnreachable() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        kafkaTemplate.send(KafkaTopics.CLAIM_SUBMITTED, claimId.toString(),
                ClaimSubmittedEvent.builder()
                        .claimId(claimId).userId(userId).policyId(UUID.randomUUID())
                        .claimNumber("CLM-2").claimType(ClaimType.CONSULTATION).amount(BigDecimal.valueOf(600))
                        .build());

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Optional<FraudCheck> found = fraudCheckRepository.findByClaimId(claimId);
            assertThat(found).isPresent();
            FraudCheck fraudCheck = found.get();
            assertThat(fraudCheck.getRuleScore()).isEqualTo(40);
            assertThat(fraudCheck.getAiScore()).isNull();
            assertThat(fraudCheck.getFinalScore()).isEqualTo(40);
        });

        ConsumerRecord<String, Object> record = awaitRecord(KafkaTopics.FRAUD_DETECTED, claimId);
        FraudDetectedEvent event = (FraudDetectedEvent) record.value();
        assertThat(event.riskScore()).isEqualTo(40);
    }
}
