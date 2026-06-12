package com.ehi.notification.kafka;

import com.ehi.notification.service.NotificationService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFailedEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "notification-service")
    public void consume(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent for paymentId={}, userId={}", event.paymentId(), event.userId());

        notificationService.send(
                event.userId(),
                NotificationType.PAYMENT_FAILED,
                NotificationChannel.EMAIL,
                event.userId().toString(),
                "Payment Failed",
                "Your payment of " + event.amount() + " failed: " + event.reason());
    }
}
