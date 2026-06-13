package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.PolicyCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyCreatedEventConsumer {

    private final NotificationService notificationService;
    private final UserContactRepository userContactRepository;

    @KafkaListener(topics = KafkaTopics.POLICY_CREATED, groupId = "notification-service")
    public void consume(PolicyCreatedEvent event) {
        log.info("Received PolicyCreatedEvent for policyId={}, userId={}", event.policyId(), event.userId());

        Optional<UserContact> contact = userContactRepository.findByUserId(event.userId());
        if (contact.isEmpty()) {
            log.warn("No contact for userId={}, skipping POLICY_PENDING notification", event.userId());
            return;
        }

        String subject = "Policy Application Received";
        String body = "Your policy application " + event.policyNumber() + " has been received and is pending payment of "
                + event.premiumAmount() + ".";

        notificationService.send(event.policyId(), event.userId(), NotificationType.POLICY_PENDING,
                NotificationChannel.EMAIL, contact.get().getEmail(), subject, body);

        if (contact.get().getPhone() != null) {
            notificationService.send(event.policyId(), event.userId(), NotificationType.POLICY_PENDING,
                    NotificationChannel.SMS, contact.get().getPhone(), subject, body);
        }
    }
}
