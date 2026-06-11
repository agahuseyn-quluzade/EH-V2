package com.eHealthInsurance.outbox;

import com.eHealthInsurance.common.events.EventSchemaVersions;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class EventMetadataFactory {
    private final String producer;

    public EventMetadataFactory(@Value("${spring.application.name:unknown-service}") String producer) {
        this.producer = producer;
    }

    public EventMetadata create(UUID aggregateId, UUID userId) {
        return new EventMetadata(
                EventSchemaVersions.V1,
                UUID.randomUUID(),
                Instant.now(),
                valueOrRandom(MDC.get("traceId")),
                valueOrNull(MDC.get("spanId")),
                producer,
                aggregateId,
                userId
        );
    }

    private String valueOrRandom(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }

    private String valueOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

