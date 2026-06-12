package com.ehi.notification.kafka;

import com.ehi.notification.service.NotificationService;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.event.PaymentCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentCompletedEventConsumerTest {

    @Mock NotificationService notificationService;

    @InjectMocks PaymentCompletedEventConsumer consumer;

    @Test
    void consume_sendsPolicyActivatedNotification_forPolicyPremium() {
        UUID userId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(UUID.randomUUID()).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.POLICY_PREMIUM).amount(BigDecimal.valueOf(100))
                .transactionId("TXN-1")
                .build();

        consumer.consume(event);

        verify(notificationService).send(userId, NotificationType.POLICY_ACTIVATED, NotificationChannel.EMAIL,
                userId.toString(), "Policy Activated",
                "Your policy is now active. Payment of 100 was completed (transaction TXN-1).");
    }

    @Test
    void consume_sendsPaymentSuccessNotification_forClaimPayout() {
        UUID userId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(UUID.randomUUID()).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.CLAIM_PAYOUT).amount(BigDecimal.valueOf(250))
                .transactionId("TXN-2")
                .build();

        consumer.consume(event);

        verify(notificationService).send(userId, NotificationType.PAYMENT_SUCCESS, NotificationChannel.EMAIL,
                userId.toString(), "Claim Payout Completed",
                "Your claim payout of 250 has been completed (transaction TXN-2).");
    }
}
