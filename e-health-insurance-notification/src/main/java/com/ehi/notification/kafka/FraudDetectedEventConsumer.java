package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.FraudDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDetectedEventConsumer {

    private static final int FRAUD_ALERT_THRESHOLD = 70;

    private final NotificationService notificationService;
    private final UserContactRepository userContactRepository;

    @KafkaListener(topics = KafkaTopics.FRAUD_DETECTED, groupId = "notification-service")
    public void consume(FraudDetectedEvent event) {
        if (event.riskScore() == null || event.riskScore() < FRAUD_ALERT_THRESHOLD) {
            return;
        }

        log.info("Received FraudDetectedEvent for claimId={}, userId={}, riskScore={}", event.claimId(), event.userId(), event.riskScore());

        Optional<UserContact> contact = userContactRepository.findByUserId(event.userId());
        if (contact.isEmpty()) {
            log.warn("No contact for userId={}, skipping FRAUD_ALERT notification", event.userId());
            return;
        }

        notificationService.send(
                event.claimId(),
                event.userId(),
                NotificationType.FRAUD_ALERT,
                NotificationChannel.EMAIL,
                contact.get().getEmail(),
                "Claim Flagged for Review",
                "Your claim has been flagged for additional review (risk score: " + event.riskScore() + "). "
                        + event.aiExplanation());
    }
}
