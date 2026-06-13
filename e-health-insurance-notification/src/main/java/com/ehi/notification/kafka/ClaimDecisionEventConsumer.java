package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
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

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimDecisionEventConsumer {

    private final NotificationService notificationService;
    private final UserContactRepository userContactRepository;

    @KafkaListener(topics = KafkaTopics.CLAIM_DECISION, groupId = "notification-service")
    public void consume(ClaimDecisionEvent event) {
        log.info("Received ClaimDecisionEvent for claimId={}, userId={}, decision={}", event.claimId(), event.userId(), event.decision());

        if (event.decision() != ClaimStatus.APPROVED && event.decision() != ClaimStatus.REJECTED) {
            return;
        }

        Optional<UserContact> contact = userContactRepository.findByUserId(event.userId());
        if (contact.isEmpty()) {
            log.warn("No contact for userId={}, skipping claim decision notification", event.userId());
            return;
        }

        NotificationType type;
        String subject;
        String body;

        if (event.decision() == ClaimStatus.APPROVED) {
            type = NotificationType.CLAIM_APPROVED;
            subject = "Claim Approved";
            body = "Your claim has been approved for a payout of " + event.approvedAmount() + ".";
        } else {
            type = NotificationType.CLAIM_REJECTED;
            subject = "Claim Rejected";
            body = "Your claim was rejected: " + event.rejectionReason();
        }

        notificationService.send(event.claimId(), event.userId(), type,
                NotificationChannel.EMAIL, contact.get().getEmail(), subject, body);

        if (contact.get().getPhone() != null) {
            notificationService.send(event.claimId(), event.userId(), type,
                    NotificationChannel.SMS, contact.get().getPhone(), subject, body);
        }
    }
}
