package com.eHealthInsurance.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "ehi.outbox.publisher", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties properties;

    @Scheduled(fixedDelayString = "${ehi.outbox.publisher.fixed-delay-ms:5000}")
    @Transactional
    public void publishPending() {
        if (!properties.isEnabled()) {
            return;
        }
        List<OutboxEvent> batch = repository.findNextBatchForUpdate(Instant.now(), properties.getBatchSize());
        for (OutboxEvent event : batch) {
            publishOne(event);
            repository.save(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        String targetTopic = event.getStatus() == OutboxEventStatus.DLQ_PENDING
                ? event.getTopic() + properties.getDlqSuffix()
                : event.getTopic();
        try {
            kafkaTemplate.send(targetTopic, event.getEventId().toString(), event.getPayload())
                    .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (event.getStatus() == OutboxEventStatus.DLQ_PENDING) {
                event.markDlqSent();
                log.warn("Outbox event {} published to DLQ topic {}", event.getEventId(), targetTopic);
            } else {
                event.markSent();
                log.debug("Outbox event {} published to topic {}", event.getEventId(), targetTopic);
            }
        } catch (Exception ex) {
            if (event.getStatus() == OutboxEventStatus.DLQ_PENDING) {
                event.markDlqFailed(ex, properties.retryDelay());
            } else {
                event.markFailed(ex, properties.retryDelay());
            }
            log.warn("Outbox event {} publish failed for topic {}: {}", event.getEventId(), targetTopic, ex.getMessage());
        }
    }
}

