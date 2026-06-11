package com.eHealthInsurance.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {
    private static final int MAX_ERROR_LENGTH = 4000;

    @Id
    @Column(name = "event_id", nullable = false, columnDefinition = "uuid")
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, columnDefinition = "uuid")
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 160)
    private String eventType;

    @Column(nullable = false, length = 160)
    private String topic;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public static OutboxEvent pending(
            UUID eventId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String topic,
            String payload,
            int maxAttempts
    ) {
        OutboxEvent event = new OutboxEvent();
        event.eventId = eventId;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.topic = topic;
        event.payload = payload;
        event.status = OutboxEventStatus.PENDING;
        event.maxAttempts = maxAttempts;
        event.nextAttemptAt = Instant.now();
        return event;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = OutboxEventStatus.PENDING;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = createdAt;
        }
    }

    public void markSent() {
        this.status = OutboxEventStatus.SENT;
        this.sentAt = Instant.now();
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public void markFailed(Throwable failure, Duration retryDelay) {
        this.retryCount++;
        this.lastError = abbreviate(failure);
        if (this.retryCount >= this.maxAttempts) {
            this.status = OutboxEventStatus.DLQ_PENDING;
        } else {
            this.status = OutboxEventStatus.FAILED;
        }
        this.nextAttemptAt = Instant.now().plus(backoff(retryDelay));
    }

    public void markDlqSent() {
        this.status = OutboxEventStatus.DLQ_SENT;
        this.sentAt = Instant.now();
        this.nextAttemptAt = null;
    }

    public void markDlqFailed(Throwable failure, Duration retryDelay) {
        this.retryCount++;
        this.status = OutboxEventStatus.DLQ_PENDING;
        this.lastError = abbreviate(failure);
        this.nextAttemptAt = Instant.now().plus(backoff(retryDelay));
    }

    private Duration backoff(Duration retryDelay) {
        long multiplier = Math.max(1, Math.min(retryCount, 10));
        return retryDelay.multipliedBy(multiplier);
    }

    private String abbreviate(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            message = failure.getClass().getName();
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }

    public UUID getEventId() { return eventId; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public String getPayload() { return payload; }
    public OutboxEventStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
}
