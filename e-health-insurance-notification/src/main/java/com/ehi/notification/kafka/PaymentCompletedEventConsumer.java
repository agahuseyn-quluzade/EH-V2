package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
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

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedEventConsumer {

    private final NotificationService notificationService;
    private final UserContactRepository userContactRepository;

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "notification-service")
    public void consume(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for paymentId={}, userId={}", event.paymentId(), event.userId());

        Optional<UserContact> contact = userContactRepository.findByUserId(event.userId());
        if (contact.isEmpty()) {
            log.warn("No contact for userId={}, skipping payment notification", event.userId());
            return;
        }

        String recipient = contact.get().getPhone() != null ? contact.get().getPhone() : contact.get().getEmail();
        NotificationChannel channel = contact.get().getPhone() != null ? NotificationChannel.SMS : NotificationChannel.EMAIL;

        if (event.referenceType() == PaymentReferenceType.POLICY_PREMIUM) {
            notificationService.send(
                    event.paymentId(),
                    event.userId(),
                    NotificationType.POLICY_ACTIVATED,
                    channel,
                    recipient,
                    "Policy Activated",
                    "Your policy is now active. Payment of " + event.amount() + " was completed (transaction "
                            + event.transactionId() + ").");
        } else {
            notificationService.send(
                    event.paymentId(),
                    event.userId(),
                    NotificationType.PAYMENT_SUCCESS,
                    channel,
                    recipient,
                    "Claim Payout Completed",
                    "Your claim payout of " + event.amount() + " has been completed (transaction "
                            + event.transactionId() + ").");
        }
    }
}
