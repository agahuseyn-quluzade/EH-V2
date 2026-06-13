package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCompletedEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock UserContactRepository userContactRepository;

    @InjectMocks PaymentCompletedEventConsumer consumer;

    @Test
    void consume_sendsPolicyActivatedNotification_viaSms_forPolicyPremium() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserContact contact = UserContact.builder().userId(userId).email("user@example.com").phone("+994501234567").build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.of(contact));

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(paymentId).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.POLICY_PREMIUM).amount(BigDecimal.valueOf(100))
                .transactionId("TXN-1")
                .build();

        consumer.consume(event);

        verify(notificationService).send(paymentId, userId, NotificationType.POLICY_ACTIVATED, NotificationChannel.SMS,
                "+994501234567", "Policy Activated",
                "Your policy is now active. Payment of 100 was completed (transaction TXN-1).");
    }

    @Test
    void consume_sendsPaymentSuccessNotification_viaSms_forClaimPayout() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserContact contact = UserContact.builder().userId(userId).email("user@example.com").phone("+994501234567").build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.of(contact));

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(paymentId).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.CLAIM_PAYOUT).amount(BigDecimal.valueOf(250))
                .transactionId("TXN-2")
                .build();

        consumer.consume(event);

        verify(notificationService).send(paymentId, userId, NotificationType.PAYMENT_SUCCESS, NotificationChannel.SMS,
                "+994501234567", "Claim Payout Completed",
                "Your claim payout of 250 has been completed (transaction TXN-2).");
    }

    @Test
    void consume_sendsNothing_whenContactNotFound() {
        UUID userId = UUID.randomUUID();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.empty());

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(UUID.randomUUID()).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.POLICY_PREMIUM).amount(BigDecimal.valueOf(100))
                .transactionId("TXN-3")
                .build();

        consumer.consume(event);

        verifyNoInteractions(notificationService);
    }
}
