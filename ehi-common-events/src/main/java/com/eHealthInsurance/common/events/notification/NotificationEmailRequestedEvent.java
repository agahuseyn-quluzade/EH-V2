package com.eHealthInsurance.common.events.notification;

import com.eHealthInsurance.common.events.DomainEvent;
import com.eHealthInsurance.common.events.EventTopics;
import com.eHealthInsurance.common.events.EventValidation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationEmailRequestedEvent(
        String schemaVersion,
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        String causationId,
        String producer,
        UUID aggregateId,
        UUID userId,
        UUID notificationId,
        UUID recipientId,
        String recipientEmail,
        String template,
        String subject,
        Map<String, Object> templateVariables
) implements DomainEvent {
    public static final String EVENT_TYPE = EventTopics.NOTIFICATION_EMAIL_REQUESTED;

    public NotificationEmailRequestedEvent {
        EventValidation.requireMetadata(schemaVersion, eventId, occurredAt, correlationId, producer, aggregateId);
        EventValidation.requireNonNull(notificationId, "notificationId");
        EventValidation.requireNonNull(recipientId, "recipientId");
        EventValidation.requireText(recipientEmail, "recipientEmail");
        EventValidation.requireText(template, "template");
        EventValidation.requireText(subject, "subject");
        if (!aggregateId.equals(notificationId)) {
            throw new IllegalArgumentException("aggregateId must match notificationId");
        }
    }

    @Override
    public String topic() {
        return EVENT_TYPE;
    }
}
