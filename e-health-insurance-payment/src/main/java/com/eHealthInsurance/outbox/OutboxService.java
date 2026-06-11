package com.eHealthInsurance.outbox;

import com.eHealthInsurance.common.events.DomainEvent;
import com.eHealthInsurance.common.events.json.EventJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxEventRepository repository;
    private final OutboxProperties properties;

    public OutboxEvent enqueue(DomainEvent event) {
        try {
            OutboxEvent outboxEvent = OutboxEvent.pending(
                    event.eventId(),
                    aggregateType(event),
                    event.aggregateId(),
                    event.topic(),
                    event.topic(),
                    EventJson.write(event),
                    properties.getMaxAttempts()
            );
            return repository.save(outboxEvent);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Event payload serialization failed: " + event.topic(), ex);
        }
    }

    private String aggregateType(DomainEvent event) {
        String simpleName = event.getClass().getSimpleName();
        return simpleName.endsWith("Event") ? simpleName.substring(0, simpleName.length() - 5) : simpleName;
    }
}
