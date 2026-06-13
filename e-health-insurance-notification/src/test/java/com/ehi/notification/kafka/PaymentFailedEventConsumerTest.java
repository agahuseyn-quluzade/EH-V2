package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentFailedEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock UserContactRepository userContactRepository;

    @InjectMocks PaymentFailedEventConsumer consumer;

    @Test
    void consume_sendsPaymentFailedNotification_viaSms_whenPhoneAvailable() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserContact contact = UserContact.builder().userId(userId).email("user@example.com").phone("+994501234567").build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.of(contact));

        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(paymentId).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.POLICY_PREMIUM).amount(BigDecimal.valueOf(100))
                .reason("Insufficient funds")
                .build();

        consumer.consume(event);

        verify(notificationService).send(paymentId, userId, NotificationType.PAYMENT_FAILED, NotificationChannel.SMS,
                "+994501234567", "Payment Failed", "Your payment of 100 failed: Insufficient funds");
    }

    @Test
    void consume_sendsPaymentFailedNotification_viaEmail_whenNoPhone() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserContact contact = UserContact.builder().userId(userId).email("user@example.com").phone(null).build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.of(contact));

        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(paymentId).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.POLICY_PREMIUM).amount(BigDecimal.valueOf(100))
                .reason("Card declined")
                .build();

        consumer.consume(event);

        verify(notificationService).send(paymentId, userId, NotificationType.PAYMENT_FAILED, NotificationChannel.EMAIL,
                "user@example.com", "Payment Failed", "Your payment of 100 failed: Card declined");
    }

    @Test
    void consume_sendsNothing_whenContactNotFound() {
        UUID userId = UUID.randomUUID();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.empty());

        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(UUID.randomUUID()).userId(userId).referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.POLICY_PREMIUM).amount(BigDecimal.valueOf(100))
                .reason("Insufficient funds")
                .build();

        consumer.consume(event);

        verifyNoInteractions(notificationService);
    }
}
