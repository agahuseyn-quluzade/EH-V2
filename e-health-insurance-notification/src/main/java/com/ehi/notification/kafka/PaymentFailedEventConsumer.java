package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFailedEventConsumer {

    private final NotificationService notificationService;
    private final UserContactRepository userContactRepository;

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "notification-service")
    public void consume(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent for paymentId={}, userId={}", event.paymentId(), event.userId());

        Optional<UserContact> contact = userContactRepository.findByUserId(event.userId());
        if (contact.isEmpty()) {
            log.warn("No contact for userId={}, skipping PAYMENT_FAILED notification", event.userId());
            return;
        }

        String recipient = contact.get().getPhone() != null ? contact.get().getPhone() : contact.get().getEmail();
        NotificationChannel channel = contact.get().getPhone() != null ? NotificationChannel.SMS : NotificationChannel.EMAIL;

        notificationService.send(
                event.paymentId(),
                event.userId(),
                NotificationType.PAYMENT_FAILED,
                channel,
                recipient,
                "Payment Failed",
                "Your payment of " + event.amount() + " failed: " + event.reason());
    }
}
