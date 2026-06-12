package com.ehi.notification.kafka;

import com.ehi.notification.service.NotificationService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.ClaimSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimSubmittedEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.CLAIM_SUBMITTED, groupId = "notification-service")
    public void consume(ClaimSubmittedEvent event) {
        log.info("Received ClaimSubmittedEvent for claimId={}, userId={}", event.claimId(), event.userId());

        notificationService.send(
                event.userId(),
                NotificationType.CLAIM_SUBMITTED,
                NotificationChannel.EMAIL,
                event.userId().toString(),
                "Claim Submitted",
                "Your " + event.claimType() + " claim " + event.claimNumber() + " for " + event.amount()
                        + " has been submitted and is under review.");
    }
}
