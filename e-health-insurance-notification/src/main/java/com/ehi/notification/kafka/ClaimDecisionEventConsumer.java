package com.ehi.notification.kafka;

import com.ehi.notification.service.NotificationService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.ClaimStatus;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.ClaimDecisionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimDecisionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.CLAIM_DECISION, groupId = "notification-service")
    public void consume(ClaimDecisionEvent event) {
        log.info("Received ClaimDecisionEvent for claimId={}, userId={}, decision={}", event.claimId(), event.userId(), event.decision());

        if (event.decision() == ClaimStatus.APPROVED) {
            notificationService.send(
                    event.userId(),
                    NotificationType.CLAIM_APPROVED,
                    NotificationChannel.EMAIL,
                    event.userId().toString(),
                    "Claim Approved",
                    "Your claim has been approved for a payout of " + event.approvedAmount() + ".");
        } else if (event.decision() == ClaimStatus.REJECTED) {
            notificationService.send(
                    event.userId(),
                    NotificationType.CLAIM_REJECTED,
                    NotificationChannel.EMAIL,
                    event.userId().toString(),
                    "Claim Rejected",
                    "Your claim was rejected: " + event.rejectionReason());
        }
    }
}
