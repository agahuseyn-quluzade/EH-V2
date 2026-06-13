package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.ClaimSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimSubmittedEventConsumer {

    private final NotificationService notificationService;
    private final UserContactRepository userContactRepository;

    @KafkaListener(topics = KafkaTopics.CLAIM_SUBMITTED, groupId = "notification-service")
    public void consume(ClaimSubmittedEvent event) {
        log.info("Received ClaimSubmittedEvent for claimId={}, userId={}", event.claimId(), event.userId());

        Optional<UserContact> contact = userContactRepository.findByUserId(event.userId());
        if (contact.isEmpty()) {
            log.warn("No contact for userId={}, skipping CLAIM_SUBMITTED notification", event.userId());
            return;
        }

        String subject = "Claim Submitted";
        String body = "Your " + event.claimType() + " claim " + event.claimNumber() + " for " + event.amount()
                + " has been submitted and is under review.";

        notificationService.send(event.claimId(), event.userId(), NotificationType.CLAIM_SUBMITTED,
                NotificationChannel.EMAIL, contact.get().getEmail(), subject, body);

        if (contact.get().getPhone() != null) {
            notificationService.send(event.claimId(), event.userId(), NotificationType.CLAIM_SUBMITTED,
                    NotificationChannel.SMS, contact.get().getPhone(), subject, body);
        }
    }
}
