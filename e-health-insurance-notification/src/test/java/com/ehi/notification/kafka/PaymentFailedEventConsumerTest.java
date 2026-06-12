package com.ehi.notification.kafka;

import com.ehi.notification.service.NotificationService;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.event.PaymentFailedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentFailedEventConsumerTest {

    @Mock NotificationService notificationService;

    @InjectMocks PaymentFailedEventConsumer consumer;

    @Test
    void consume_sendsPaymentFailedNotification() {
        UUID userId = UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(UUID.randomUUID()).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.POLICY_PREMIUM).amount(BigDecimal.valueOf(100))
                .reason("Insufficient funds")
                .build();

        consumer.consume(event);

        verify(notificationService).send(userId, NotificationType.PAYMENT_FAILED, NotificationChannel.EMAIL,
                userId.toString(), "Payment Failed", "Your payment of 100 failed: Insufficient funds");
    }
}
