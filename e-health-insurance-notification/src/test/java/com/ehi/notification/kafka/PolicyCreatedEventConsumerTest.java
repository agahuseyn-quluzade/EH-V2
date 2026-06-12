package com.ehi.notification.kafka;

import com.ehi.notification.service.NotificationService;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.PolicyCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PolicyCreatedEventConsumerTest {

    @Mock NotificationService notificationService;

    @InjectMocks PolicyCreatedEventConsumer consumer;

    @Test
    void consume_sendsPolicyPendingNotification() {
        UUID userId = UUID.randomUUID();
        PolicyCreatedEvent event = PolicyCreatedEvent.builder()
                .policyId(UUID.randomUUID()).userId(userId).planId(UUID.randomUUID())
                .policyNumber("POL-1").premiumAmount(BigDecimal.valueOf(100))
                .build();

        consumer.consume(event);

        verify(notificationService).send(userId, NotificationType.POLICY_PENDING, NotificationChannel.EMAIL,
                userId.toString(), "Policy Application Received",
                "Your policy application POL-1 has been received and is pending payment of 100.");
    }
}
