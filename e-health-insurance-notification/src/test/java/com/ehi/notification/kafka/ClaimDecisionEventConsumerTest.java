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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimDecisionEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock UserContactRepository userContactRepository;

    @InjectMocks ClaimDecisionEventConsumer consumer;

    @Test
    void consume_sendsApprovedEmail_whenApproved() {
        UUID userId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        ClaimDecisionEvent event = ClaimDecisionEvent.builder()
                .claimId(claimId).userId(userId).policyId(UUID.randomUUID())
                .decision(ClaimStatus.APPROVED).approvedAmount(BigDecimal.valueOf(500)).build();
        when(userContactRepository.findByUserId(userId)).thenReturn(
                Optional.of(UserContact.builder().userId(userId).email("user@example.com").build()));

        consumer.consume(event);

        verify(notificationService).send(claimId, userId, NotificationType.CLAIM_APPROVED,
                NotificationChannel.EMAIL, "user@example.com", "Claim Approved",
                "Your claim has been approved for a payout of 500.");
        verify(notificationService, times(1)).send(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_sendsRejectedEmail_whenRejected() {
        UUID userId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        ClaimDecisionEvent event = ClaimDecisionEvent.builder()
                .claimId(claimId).userId(userId).policyId(UUID.randomUUID())
                .decision(ClaimStatus.REJECTED).rejectionReason("Not covered under policy").build();
        when(userContactRepository.findByUserId(userId)).thenReturn(
                Optional.of(UserContact.builder().userId(userId).email("user@example.com").build()));

        consumer.consume(event);

        verify(notificationService).send(claimId, userId, NotificationType.CLAIM_REJECTED,
                NotificationChannel.EMAIL, "user@example.com", "Claim Rejected",
                "Your claim was rejected: Not covered under policy");
        verify(notificationService, times(1)).send(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_sendsEmailAndSms_whenPhonePresent() {
        UUID userId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        ClaimDecisionEvent event = ClaimDecisionEvent.builder()
                .claimId(claimId).userId(userId).policyId(UUID.randomUUID())
                .decision(ClaimStatus.APPROVED).approvedAmount(BigDecimal.valueOf(500)).build();
        when(userContactRepository.findByUserId(userId)).thenReturn(
                Optional.of(UserContact.builder().userId(userId).email("user@example.com").phone("+994501234567").build()));

        consumer.consume(event);

        verify(notificationService, times(2)).send(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void consume_ignores_underReviewDecision() {
        UUID userId = UUID.randomUUID();
        ClaimDecisionEvent event = ClaimDecisionEvent.builder()
                .claimId(UUID.randomUUID()).userId(userId).policyId(UUID.randomUUID())
                .decision(ClaimStatus.UNDER_REVIEW).build();

        consumer.consume(event);

        verifyNoInteractions(notificationService);
        verifyNoInteractions(userContactRepository);
    }

    @Test
    void consume_skips_whenNoContact() {
        UUID userId = UUID.randomUUID();
        ClaimDecisionEvent event = ClaimDecisionEvent.builder()
                .claimId(UUID.randomUUID()).userId(userId).policyId(UUID.randomUUID())
                .decision(ClaimStatus.APPROVED).approvedAmount(BigDecimal.valueOf(500)).build();
        when(userContactRepository.findByUserId(userId)).thenReturn(Optional.empty());

        consumer.consume(event);

        verifyNoInteractions(notificationService);
    }
}
