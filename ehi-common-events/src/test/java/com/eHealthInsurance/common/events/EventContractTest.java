package com.eHealthInsurance.common.events;

import com.eHealthInsurance.common.events.audit.AuditEventCreatedEvent;
import com.eHealthInsurance.common.events.claim.ClaimAiScoredEvent;
import com.eHealthInsurance.common.events.claim.ClaimApprovedEvent;
import com.eHealthInsurance.common.events.claim.ClaimRejectedEvent;
import com.eHealthInsurance.common.events.claim.ClaimReviewRequiredEvent;
import com.eHealthInsurance.common.events.claim.ClaimSubmittedEvent;
import com.eHealthInsurance.common.events.healthrecord.HealthRecordCreatedEvent;
import com.eHealthInsurance.common.events.healthrecord.HealthRecordUpdatedEvent;
import com.eHealthInsurance.common.events.notification.NotificationEmailRequestedEvent;
import com.eHealthInsurance.common.events.payment.PaymentFailedEvent;
import com.eHealthInsurance.common.events.payment.PaymentInitiatedEvent;
import com.eHealthInsurance.common.events.payment.PaymentRefundedEvent;
import com.eHealthInsurance.common.events.payment.PaymentSucceededEvent;
import com.eHealthInsurance.common.events.policy.PolicyActivatedEvent;
import com.eHealthInsurance.common.events.policy.PolicyCancelledEvent;
import com.eHealthInsurance.common.events.policy.PolicyPurchaseRequestedEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventContractTest {
    private static final List<Class<? extends DomainEvent>> EVENT_TYPES = List.of(
            PaymentInitiatedEvent.class,
            PaymentSucceededEvent.class,
            PaymentFailedEvent.class,
            PaymentRefundedEvent.class,
            PolicyPurchaseRequestedEvent.class,
            PolicyActivatedEvent.class,
            PolicyCancelledEvent.class,
            ClaimSubmittedEvent.class,
            ClaimAiScoredEvent.class,
            ClaimReviewRequiredEvent.class,
            ClaimApprovedEvent.class,
            ClaimRejectedEvent.class,
            NotificationEmailRequestedEvent.class,
            HealthRecordCreatedEvent.class,
            HealthRecordUpdatedEvent.class,
            AuditEventCreatedEvent.class
    );

    private static final Set<String> REQUIRED_METADATA_FIELDS = Set.of(
            "schemaVersion",
            "eventId",
            "occurredAt",
            "correlationId",
            "causationId",
            "producer",
            "aggregateId",
            "userId"
    );

    @Test
    void everyEventIsARecordWithRequiredMetadataFields() {
        for (Class<? extends DomainEvent> eventType : EVENT_TYPES) {
            assertTrue(eventType.isRecord(), eventType.getSimpleName() + " must be a Java record");
            Set<String> componentNames = Arrays.stream(eventType.getRecordComponents())
                    .map(RecordComponent::getName)
                    .collect(Collectors.toSet());
            assertTrue(componentNames.containsAll(REQUIRED_METADATA_FIELDS),
                    eventType.getSimpleName() + " is missing required metadata fields");
        }
    }

    @Test
    void topicCatalogMatchesConcreteEventContracts() throws Exception {
        Set<String> contractTopics = new LinkedHashSet<>();
        for (Class<? extends DomainEvent> eventType : EVENT_TYPES) {
            contractTopics.add((String) eventType.getField("EVENT_TYPE").get(null));
        }

        assertEquals(new LinkedHashSet<>(EventTopics.all()), contractTopics);
    }
}
