package com.ehi.notification.kafka;

import com.ehi.notification.service.NotificationService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "notification-service")
    public void consume(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for paymentId={}, userId={}", event.paymentId(), event.userId());

        if (event.referenceType() == PaymentReferenceType.POLICY_PREMIUM) {
            notificationService.send(
                    event.userId(),
                    NotificationType.POLICY_ACTIVATED,
                    NotificationChannel.EMAIL,
                    event.userId().toString(),
                    "Policy Activated",
                    "Your policy is now active. Payment of " + event.amount() + " was completed (transaction "
                            + event.transactionId() + ").");
        } else {
            notificationService.send(
                    event.userId(),
                    NotificationType.PAYMENT_SUCCESS,
                    NotificationChannel.EMAIL,
                    event.userId().toString(),
                    "Claim Payout Completed",
                    "Your claim payout of " + event.amount() + " has been completed (transaction "
                            + event.transactionId() + ").");
        }
    }
}
