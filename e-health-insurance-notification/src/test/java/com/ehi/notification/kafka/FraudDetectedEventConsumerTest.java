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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectedEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock UserContactRepository userContactRepository;

    @InjectMocks FraudDetectedEventConsumer consumer;

    @Test
    void consume_sendsFraudAlertEmail_whenRiskScoreAtThreshold() {
        UUID userId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        FraudDetectedEvent event = FraudDetectedEvent.builder()
                .claimId(claimId).userId(userId).riskScore(70)
                .flags(List.of("AMOUNT_ABOVE_TYPE_THRESHOLD")).aiExplanation("High risk claim")
                .build();
        when(userContactRepository.findByUserId(userId)).thenReturn(
                Optional.of(UserContact.builder().userId(userId).email("user@example.com").build()));

        consumer.consume(event);

        verify(notificationService).send(claimId, userId, NotificationType.FRAUD_ALERT,
                NotificationChannel.EMAIL, "user@example.com", "Claim Flagged for Review",
                "Your claim has been flagged for additional review (risk score: 70). High risk claim");
        verify(notificationService, times(1)).send(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_sendsEmailAndSms_whenPhonePresent() {
        UUID userId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        FraudDetectedEvent event = FraudDetectedEvent.builder()
                .claimId(claimId).userId(userId).riskScore(70)
                .flags(List.of("AMOUNT_ABOVE_TYPE_THRESHOLD")).aiExplanation("High risk claim")
                .build();
        when(userContactRepository.findByUserId(userId)).thenReturn(
                Optional.of(UserContact.builder().userId(userId).email("user@example.com").phone("+994501234567").build()));

        consumer.consume(event);

        verify(notificationService, times(2)).send(any(), any(), any(), any(), any(), any(), any());
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
        verifyNoInteractions(userContactRepository);
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
        verifyNoInteractions(userContactRepository);
    }

    @Test
    void consume_skips_whenNoContact() {
        UUID userId = UUID.randomUUID();
        FraudDetectedEvent event = FraudDetectedEvent.builder()
                .claimId(UUID.randomUUID()).userId(userId).riskScore(80)
                .flags(List.of()).aiExplanation("Suspicious")
                .build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.empty());

        consumer.consume(event);

        verifyNoInteractions(notificationService);
    }
}
