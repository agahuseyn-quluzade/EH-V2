package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.PolicyCreatedEvent;
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
class PolicyCreatedEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock UserContactRepository userContactRepository;

    @InjectMocks PolicyCreatedEventConsumer consumer;

    @Test
    void consume_sendsPolicyPendingNotification_viaEmail() {
        UUID policyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserContact contact = UserContact.builder().userId(userId).email("user@example.com").phone("+994501234567").build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.of(contact));

        PolicyCreatedEvent event = PolicyCreatedEvent.builder()
                .policyId(policyId).userId(userId).planId(UUID.randomUUID())
                .policyNumber("POL-1").premiumAmount(BigDecimal.valueOf(100))
                .build();

        consumer.consume(event);

        verify(notificationService).send(policyId, userId, NotificationType.POLICY_PENDING, NotificationChannel.EMAIL,
                "user@example.com", "Policy Application Received",
                "Your policy application POL-1 has been received and is pending payment of 100.");
    }

    @Test
    void consume_sendsNothing_whenContactNotFound() {
        UUID userId = UUID.randomUUID();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.empty());

        PolicyCreatedEvent event = PolicyCreatedEvent.builder()
                .policyId(UUID.randomUUID()).userId(userId).planId(UUID.randomUUID())
                .policyNumber("POL-2").premiumAmount(BigDecimal.valueOf(200))
                .build();

        consumer.consume(event);

        verifyNoInteractions(notificationService);
    }
}
