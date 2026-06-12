package com.ehi.notification.kafka;

import com.ehi.notification.service.NotificationService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.FraudDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDetectedEventConsumer {

    private static final int FRAUD_ALERT_THRESHOLD = 70;

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.FRAUD_DETECTED, groupId = "notification-service")
    public void consume(FraudDetectedEvent event) {
        if (event.riskScore() == null || event.riskScore() < FRAUD_ALERT_THRESHOLD) {
            return;
        }

        log.info("Received FraudDetectedEvent for claimId={}, userId={}, riskScore={}", event.claimId(), event.userId(), event.riskScore());

        notificationService.send(
                event.userId(),
                NotificationType.FRAUD_ALERT,
                NotificationChannel.EMAIL,
                event.userId().toString(),
                "Claim Flagged for Review",
                "Your claim has been flagged for additional review (risk score: " + event.riskScore() + "). "
                        + event.aiExplanation());
    }
}
