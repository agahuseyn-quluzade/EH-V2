package com.ehi.notification.kafka;

import com.ehi.notification.entity.UserContact;
import com.ehi.notification.repository.UserContactRepository;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.enums.ClaimStatus;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.event.ClaimDecisionEvent;
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
class ClaimDecisionEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock UserContactRepository userContactRepository;

    @InjectMocks ClaimDecisionEventConsumer consumer;

    @Test
    void consume_sendsClaimApprovedNotification_viaSms_whenApproved() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserContact contact = UserContact.builder().userId(userId).email("user@example.com").phone("+994501234567").build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.of(contact));

        ClaimDecisionEvent event = ClaimDecisionEvent.builder()
                .claimId(claimId).userId(userId).policyId(UUID.randomUUID())
                .decision(ClaimStatus.APPROVED).approvedAmount(BigDecimal.valueOf(500))
                .reviewedBy(UUID.randomUUID())
                .build();

        consumer.consume(event);

        verify(notificationService).send(claimId, userId, NotificationType.CLAIM_APPROVED, NotificationChannel.SMS,
                "+994501234567", "Claim Approved", "Your claim has been approved for a payout of 500.");
    }

    @Test
    void consume_sendsClaimRejectedNotification_viaSms_whenRejected() {
        UUID claimId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserContact contact = UserContact.builder().userId(userId).email("user@example.com").phone("+994501234567").build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.of(contact));

        ClaimDecisionEvent event = ClaimDecisionEvent.builder()
                .claimId(claimId).userId(userId).policyId(UUID.randomUUID())
                .decision(ClaimStatus.REJECTED).rejectionReason("Not covered under policy")
                .reviewedBy(UUID.randomUUID())
                .build();

        consumer.consume(event);

        verify(notificationService).send(claimId, userId, NotificationType.CLAIM_REJECTED, NotificationChannel.SMS,
                "+994501234567", "Claim Rejected", "Your claim was rejected: Not covered under policy");
    }

    @Test
    void consume_sendsNothing_whenDecisionIsUnderReview() {
        UUID userId = UUID.randomUUID();
        ClaimDecisionEvent event = ClaimDecisionEvent.builder()
                .claimId(UUID.randomUUID()).userId(userId).policyId(UUID.randomUUID())
                .decision(ClaimStatus.UNDER_REVIEW)
                .build();

        consumer.consume(event);

        verifyNoInteractions(notificationService);
    }
}
