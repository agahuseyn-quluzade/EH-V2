package com.eHealthInsurance.common.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    String schemaVersion();

    UUID eventId();

    Instant occurredAt();

    String correlationId();

    String causationId();

    String producer();

    UUID aggregateId();

    UUID userId();

    @JsonIgnore
    String topic();
}
