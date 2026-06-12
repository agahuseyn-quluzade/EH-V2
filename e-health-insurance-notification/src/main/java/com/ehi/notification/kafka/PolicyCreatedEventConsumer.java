package com.ehi.notification.kafka;

import com.ehi.notification.service.NotificationService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.PolicyCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyCreatedEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.POLICY_CREATED, groupId = "notification-service")
    public void consume(PolicyCreatedEvent event) {
        log.info("Received PolicyCreatedEvent for policyId={}, userId={}", event.policyId(), event.userId());

        notificationService.send(
                event.userId(),
                NotificationType.POLICY_PENDING,
                NotificationChannel.EMAIL,
                event.userId().toString(),
                "Policy Application Received",
                "Your policy application " + event.policyNumber() + " has been received and is pending payment of "
                        + event.premiumAmount() + ".");
    }
}
