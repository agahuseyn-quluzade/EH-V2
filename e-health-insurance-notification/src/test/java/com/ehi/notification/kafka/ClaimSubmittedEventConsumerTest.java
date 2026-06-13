package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.enums.ClaimType;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.ClaimSubmittedEvent;
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
class ClaimSubmittedEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock UserContactRepository userContactRepository;

    @InjectMocks ClaimSubmittedEventConsumer consumer;

    @Test
    void consume_sendsClaimSubmittedNotification_viaEmail() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserContact contact = UserContact.builder().userId(userId).email("user@example.com").phone("+994501234567").build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.of(contact));

        ClaimSubmittedEvent event = ClaimSubmittedEvent.builder()
                .claimId(claimId).userId(userId).policyId(UUID.randomUUID())
                .claimNumber("CLM-1").claimType(ClaimType.CONSULTATION).amount(BigDecimal.valueOf(100))
                .build();

        consumer.consume(event);

        verify(notificationService).send(claimId, userId, NotificationType.CLAIM_SUBMITTED, NotificationChannel.EMAIL,
                "user@example.com", "Claim Submitted",
                "Your CONSULTATION claim CLM-1 for 100 has been submitted and is under review.");
    }

    @Test
    void consume_sendsNothing_whenContactNotFound() {
        UUID userId = UUID.randomUUID();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.empty());

        ClaimSubmittedEvent event = ClaimSubmittedEvent.builder()
                .claimId(UUID.randomUUID()).userId(userId).policyId(UUID.randomUUID())
                .claimNumber("CLM-2").claimType(ClaimType.HOSPITALIZATION).amount(BigDecimal.valueOf(500))
                .build();

        consumer.consume(event);

        verifyNoInteractions(notificationService);
    }
}
