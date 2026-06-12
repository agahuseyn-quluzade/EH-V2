package com.ehi.notification.kafka;

import com.ehi.notification.service.NotificationService;
import com.ehi.infra.enums.ClaimType;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.ClaimSubmittedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClaimSubmittedEventConsumerTest {

    @Mock NotificationService notificationService;

    @InjectMocks ClaimSubmittedEventConsumer consumer;

    @Test
    void consume_sendsClaimSubmittedNotification() {
        UUID userId = UUID.randomUUID();
        ClaimSubmittedEvent event = ClaimSubmittedEvent.builder()
                .claimId(UUID.randomUUID()).userId(userId).policyId(UUID.randomUUID())
                .claimNumber("CLM-1").claimType(ClaimType.CONSULTATION).amount(BigDecimal.valueOf(100))
                .build();

        consumer.consume(event);

        verify(notificationService).send(userId, NotificationType.CLAIM_SUBMITTED, NotificationChannel.EMAIL,
                userId.toString(), "Claim Submitted",
                "Your CONSULTATION claim CLM-1 for 100 has been submitted and is under review.");
    }
}
