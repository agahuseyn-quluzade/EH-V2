package com.eHealthInsurance.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {
    @Mock
    private OutboxEventRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void publishesPendingEventAndMarksSent() {
        OutboxProperties properties = properties();
        OutboxEvent event = event(10);
        when(repository.findNextBatchForUpdate(any(Instant.class), eq(10))).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("payment.succeeded"), eq(event.getEventId().toString()), eq("{}")))
                .thenReturn(CompletableFuture.completedFuture(sendResult()));

        new OutboxPublisher(repository, kafkaTemplate, properties).publishPending();

        assertEquals(OutboxEventStatus.SENT, event.getStatus());
        verify(repository).save(event);
    }

    @Test
    void movesEventToDlqPendingAfterMaxAttempts() {
        OutboxProperties properties = properties();
        OutboxEvent event = event(1);
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(repository.findNextBatchForUpdate(any(Instant.class), eq(10))).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("payment.succeeded"), eq(event.getEventId().toString()), eq("{}")))
                .thenReturn(failed);

        new OutboxPublisher(repository, kafkaTemplate, properties).publishPending();

        assertEquals(OutboxEventStatus.DLQ_PENDING, event.getStatus());
        assertEquals(1, event.getRetryCount());
        verify(repository).save(event);
    }

    @Test
    void publishesDlqPendingEventToDlqTopic() {
        OutboxProperties properties = properties();
        OutboxEvent event = event(1);
        event.markFailed(new IllegalStateException("primary failed"), properties.retryDelay());
        when(repository.findNextBatchForUpdate(any(Instant.class), eq(10))).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("payment.succeeded.dlq"), eq(event.getEventId().toString()), eq("{}")))
                .thenReturn(CompletableFuture.completedFuture(sendResult()));

        new OutboxPublisher(repository, kafkaTemplate, properties).publishPending();

        assertEquals(OutboxEventStatus.DLQ_SENT, event.getStatus());
        verify(repository).save(event);
    }

    private OutboxProperties properties() {
        OutboxProperties properties = new OutboxProperties();
        properties.setBatchSize(10);
        properties.setMaxAttempts(1);
        properties.setRetryDelayMs(1);
        properties.setSendTimeoutMs(1000);
        properties.setDlqSuffix(".dlq");
        return properties;
    }

    private OutboxEvent event(int maxAttempts) {
        return OutboxEvent.pending(
                UUID.randomUUID(),
                "PaymentSucceeded",
                UUID.randomUUID(),
                "payment.succeeded",
                "payment.succeeded",
                "{}",
                maxAttempts
        );
    }

    private SendResult<String, String> sendResult() {
        return null;
    }
}
