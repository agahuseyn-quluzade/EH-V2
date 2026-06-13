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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCompletedEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock UserContactRepository userContactRepository;

    @InjectMocks PaymentCompletedEventConsumer consumer;

    @Test
    void consume_sendsPolicyActivated_forPolicyPremium() {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(paymentId).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.POLICY_PREMIUM).amount(BigDecimal.valueOf(100))
                .transactionId("TXN-1").build();
        when(userContactRepository.findByUserId(userId)).thenReturn(
                Optional.of(UserContact.builder().userId(userId).email("user@example.com").build()));

        consumer.consume(event);

        verify(notificationService).send(paymentId, userId, NotificationType.POLICY_ACTIVATED, NotificationChannel.EMAIL,
                "user@example.com", "Policy Activated",
                "Your policy is now active. Payment of 100 was completed (transaction TXN-1).");
        verify(notificationService, times(1)).send(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_sendsPaymentSuccess_forClaimPayout() {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(paymentId).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.CLAIM_PAYOUT).amount(BigDecimal.valueOf(250))
                .transactionId("TXN-2").build();
        when(userContactRepository.findByUserId(userId)).thenReturn(
                Optional.of(UserContact.builder().userId(userId).email("user@example.com").build()));

        consumer.consume(event);

        verify(notificationService).send(paymentId, userId, NotificationType.PAYMENT_SUCCESS, NotificationChannel.EMAIL,
                "user@example.com", "Claim Payout Completed",
                "Your claim payout of 250 has been completed (transaction TXN-2).");
        verify(notificationService, times(1)).send(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_skips_whenNoContact() {
        UUID userId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(UUID.randomUUID()).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.POLICY_PREMIUM).amount(BigDecimal.valueOf(100))
                .transactionId("TXN-1").build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.empty());

        consumer.consume(event);

        verifyNoInteractions(notificationService);
    }
}
