package com.eHealthInsurance.common.events;

import com.eHealthInsurance.common.events.healthrecord.HealthRecordCreatedEvent;
import com.eHealthInsurance.common.events.json.EventJson;
import com.eHealthInsurance.common.events.notification.NotificationEmailRequestedEvent;
import com.eHealthInsurance.common.events.payment.PaymentSucceededEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventJsonTest {
    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PAYMENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID POLICY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID MEMBER_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final Instant OCCURRED_AT = Instant.parse("2026-01-02T03:04:05Z");

    @Test
    void serializesEventsWithIsoInstantAndPlainBigDecimal() throws Exception {
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                EventSchemaVersions.V1,
                EVENT_ID,
                OCCURRED_AT,
                "corr-1",
                "cmd-1",
                "payment-service",
                PAYMENT_ID,
                MEMBER_ID,
                PAYMENT_ID,
                POLICY_ID,
                MEMBER_ID,
                new BigDecimal("120.50"),
                "AZN",
                "CARD",
                "provider-ref-1",
                OCCURRED_AT
        );

        String json = EventJson.write(event);

        assertTrue(json.contains("\"occurredAt\":\"2026-01-02T03:04:05Z\""));
        assertTrue(json.contains("\"amount\":120.50"));
        assertFalse(json.contains("\"topic\""));

        PaymentSucceededEvent restored = EventJson.read(json, PaymentSucceededEvent.class);
        assertEquals(event, restored);
        assertEquals(EventTopics.PAYMENT_SUCCEEDED, restored.topic());
    }

    @Test
    void ignoresUnknownFieldsForForwardCompatibility() throws Exception {
        String json = """
                {
                  "schemaVersion": "1.0",
                  "eventId": "10000000-0000-0000-0000-000000000001",
                  "occurredAt": "2026-01-02T03:04:05Z",
                  "correlationId": "corr-1",
                  "causationId": "cmd-1",
                  "producer": "payment-service",
                  "aggregateId": "20000000-0000-0000-0000-000000000001",
                  "userId": "40000000-0000-0000-0000-000000000001",
                  "paymentId": "20000000-0000-0000-0000-000000000001",
                  "policyId": "30000000-0000-0000-0000-000000000001",
                  "memberId": "40000000-0000-0000-0000-000000000001",
                  "amount": 120.50,
                  "currency": "AZN",
                  "provider": "CARD",
                  "providerReference": "provider-ref-1",
                  "paidAt": "2026-01-02T03:04:05Z",
                  "futureField": "ignored"
                }
                """;

        PaymentSucceededEvent restored = EventJson.read(json, PaymentSucceededEvent.class);

        assertEquals(new BigDecimal("120.50"), restored.amount());
        assertEquals("provider-ref-1", restored.providerReference());
    }

    @Test
    void supportsTemplateVariableMaps() throws Exception {
        UUID notificationId = UUID.fromString("50000000-0000-0000-0000-000000000001");
        NotificationEmailRequestedEvent event = new NotificationEmailRequestedEvent(
                EventSchemaVersions.V1,
                EVENT_ID,
                OCCURRED_AT,
                "corr-1",
                "cmd-1",
                "notification-service",
                notificationId,
                MEMBER_ID,
                notificationId,
                MEMBER_ID,
                "member@example.com",
                "claim-approved",
                "Claim approved",
                Map.of("claimId", "CLM-1", "amount", "120.50")
        );

        String json = EventJson.write(event);
        NotificationEmailRequestedEvent restored = EventJson.read(json, NotificationEmailRequestedEvent.class);

        assertEquals("CLM-1", restored.templateVariables().get("claimId"));
    }

    @Test
    void rejectsMissingRequiredMetadata() {
        UUID recordId = UUID.fromString("60000000-0000-0000-0000-000000000001");

        assertThrows(IllegalArgumentException.class, () -> new HealthRecordCreatedEvent(
                EventSchemaVersions.V1,
                EVENT_ID,
                OCCURRED_AT,
                " ",
                "cmd-1",
                "health-record-service",
                recordId,
                MEMBER_ID,
                recordId,
                MEMBER_ID,
                "ACTIVE",
                OCCURRED_AT
        ));
    }
}
