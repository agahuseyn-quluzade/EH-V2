package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.FraudDetectedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectedEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock UserContactRepository userContactRepository;

    @InjectMocks FraudDetectedEventConsumer consumer;

    @Test
    void consume_sendsFraudAlertNotification_viaEmail_whenRiskScoreAtThreshold() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserContact contact = UserContact.builder().userId(userId).email("user@example.com").phone(null).build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.of(contact));

        FraudDetectedEvent event = FraudDetectedEvent.builder()
                .claimId(claimId).userId(userId).riskScore(70)
                .flags(List.of("AMOUNT_ABOVE_TYPE_THRESHOLD")).aiExplanation("High risk claim")
                .build();

        consumer.consume(event);

        verify(notificationService).send(claimId, userId, NotificationType.FRAUD_ALERT, NotificationChannel.EMAIL,
                "user@example.com", "Claim Flagged for Review",
                "Your claim has been flagged for additional review (risk score: 70). High risk claim");
    }

    @Test
    void consume_sendsNothing_whenRiskScoreBelowThreshold() {
        UUID userId = UUID.randomUUID();
        FraudDetectedEvent event = FraudDetectedEvent.builder()
                .claimId(UUID.randomUUID()).userId(userId).riskScore(69)
                .flags(List.of()).aiExplanation("Low risk")
                .build();

        consumer.consume(event);

        verifyNoInteractions(notificationService);
    }

    @Test
    void consume_sendsNothing_whenRiskScoreIsNull() {
        UUID userId = UUID.randomUUID();
        FraudDetectedEvent event = FraudDetectedEvent.builder()
                .claimId(UUID.randomUUID()).userId(userId).riskScore(null)
                .flags(List.of()).aiExplanation(null)
                .build();

        consumer.consume(event);

        verifyNoInteractions(notificationService);
    }
}
