package com.ehi.notification;

import com.ehi.notification.entity.Notification;
import com.ehi.notification.entity.UserContact;
import com.ehi.notification.enums.NotificationStatus;
import com.ehi.notification.repository.NotificationRepository;
import com.ehi.notification.repository.UserContactRepository;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.ClaimStatus;
import com.ehi.infra.enums.ClaimType;
import com.ehi.infra.enums.NotificationType;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.event.ClaimDecisionEvent;
import com.ehi.infra.event.ClaimSubmittedEvent;
import com.ehi.infra.event.FraudDetectedEvent;
import com.ehi.infra.event.PaymentCompletedEvent;
import com.ehi.infra.event.PaymentFailedEvent;
import com.ehi.infra.event.PolicyCreatedEvent;
import com.ehi.infra.event.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
@EmbeddedKafka(partitions = 1, topics = {
        KafkaTopics.USER_REGISTERED, KafkaTopics.POLICY_CREATED, KafkaTopics.PAYMENT_COMPLETED,
        KafkaTopics.PAYMENT_FAILED, KafkaTopics.CLAIM_SUBMITTED, KafkaTopics.CLAIM_DECISION, KafkaTopics.FRAUD_DETECTED
})
class NotificationFlowIT {

    @Autowired KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired NotificationRepository notificationRepository;
    @Autowired UserContactRepository userContactRepository;

    private void seedContact(UUID userId) {
        userContactRepository.save(UserContact.builder().userId(userId).email("user@example.com").build());
    }

    private Notification awaitNotification(UUID userId, NotificationType type) {
        return await().atMost(Duration.ofSeconds(10)).until(() -> {
            List<Notification> notifications = notificationRepository.findByUserId(userId);
            return notifications.stream()
                    .filter(n -> n.getType() == type && n.getStatus() != NotificationStatus.PENDING)
                    .findFirst();
        }, Optional::isPresent).get();
    }

    @Test
    void userRegistered_createsWelcomeNotification_withEmailRecipient() {
        UUID userId = UUID.randomUUID();
        kafkaTemplate.send(KafkaTopics.USER_REGISTERED, userId.toString(),
                new UserRegisteredEvent(userId, "user@example.com", "John", "Doe", null));

        Notification notification = awaitNotification(userId, NotificationType.WELCOME);

        assertThat(notification.getRecipient()).isEqualTo("user@example.com");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void policyCreated_createsPolicyPendingNotification() {
        UUID userId = UUID.randomUUID();
        seedContact(userId);
        kafkaTemplate.send(KafkaTopics.POLICY_CREATED, userId.toString(),
                PolicyCreatedEvent.builder()
                        .policyId(UUID.randomUUID()).userId(userId).planId(UUID.randomUUID())
                        .policyNumber("POL-1").premiumAmount(BigDecimal.valueOf(100))
                        .build());

        Notification notification = awaitNotification(userId, NotificationType.POLICY_PENDING);

        assertThat(notification.getSubject()).isEqualTo("Policy Application Received");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void paymentCompleted_createsPolicyActivatedNotification_forPolicyPremium() {
        UUID userId = UUID.randomUUID();
        seedContact(userId);
        kafkaTemplate.send(KafkaTopics.PAYMENT_COMPLETED, userId.toString(),
                PaymentCompletedEvent.builder()
                        .paymentId(UUID.randomUUID()).userId(userId).referenceId(UUID.randomUUID())
                        .referenceType(PaymentReferenceType.POLICY_PREMIUM).amount(BigDecimal.valueOf(100))
                        .transactionId("TXN-1")
                        .build());

        Notification notification = awaitNotification(userId, NotificationType.POLICY_ACTIVATED);

        assertThat(notification.getSubject()).isEqualTo("Policy Activated");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void paymentCompleted_createsPaymentSuccessNotification_forClaimPayout() {
        UUID userId = UUID.randomUUID();
        seedContact(userId);
        kafkaTemplate.send(KafkaTopics.PAYMENT_COMPLETED, userId.toString(),
                PaymentCompletedEvent.builder()
                        .paymentId(UUID.randomUUID()).userId(userId).referenceId(UUID.randomUUID())
                        .referenceType(PaymentReferenceType.CLAIM_PAYOUT).amount(BigDecimal.valueOf(250))
                        .transactionId("TXN-2")
                        .build());

        Notification notification = awaitNotification(userId, NotificationType.PAYMENT_SUCCESS);

        assertThat(notification.getSubject()).isEqualTo("Claim Payout Completed");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void paymentFailed_createsPaymentFailedNotification() {
        UUID userId = UUID.randomUUID();
        seedContact(userId);
        kafkaTemplate.send(KafkaTopics.PAYMENT_FAILED, userId.toString(),
                PaymentFailedEvent.builder()
                        .paymentId(UUID.randomUUID()).userId(userId).referenceId(UUID.randomUUID())
                        .referenceType(PaymentReferenceType.POLICY_PREMIUM).amount(BigDecimal.valueOf(100))
                        .reason("Insufficient funds")
                        .build());

        Notification notification = awaitNotification(userId, NotificationType.PAYMENT_FAILED);

        assertThat(notification.getSubject()).isEqualTo("Payment Failed");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void claimSubmitted_createsClaimSubmittedNotification() {
        UUID userId = UUID.randomUUID();
        seedContact(userId);
        kafkaTemplate.send(KafkaTopics.CLAIM_SUBMITTED, userId.toString(),
                ClaimSubmittedEvent.builder()
                        .claimId(UUID.randomUUID()).userId(userId).policyId(UUID.randomUUID())
                        .claimNumber("CLM-1").claimType(ClaimType.CONSULTATION).amount(BigDecimal.valueOf(100))
                        .build());

        Notification notification = awaitNotification(userId, NotificationType.CLAIM_SUBMITTED);

        assertThat(notification.getSubject()).isEqualTo("Claim Submitted");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void claimDecision_createsClaimApprovedNotification_whenApproved() {
        UUID userId = UUID.randomUUID();
        seedContact(userId);
        kafkaTemplate.send(KafkaTopics.CLAIM_DECISION, userId.toString(),
                ClaimDecisionEvent.builder()
                        .claimId(UUID.randomUUID()).userId(userId).policyId(UUID.randomUUID())
                        .decision(ClaimStatus.APPROVED).approvedAmount(BigDecimal.valueOf(500))
                        .reviewedBy(UUID.randomUUID())
                        .build());

        Notification notification = awaitNotification(userId, NotificationType.CLAIM_APPROVED);

        assertThat(notification.getSubject()).isEqualTo("Claim Approved");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void claimDecision_createsClaimRejectedNotification_whenRejected() {
        UUID userId = UUID.randomUUID();
        seedContact(userId);
        kafkaTemplate.send(KafkaTopics.CLAIM_DECISION, userId.toString(),
                ClaimDecisionEvent.builder()
                        .claimId(UUID.randomUUID()).userId(userId).policyId(UUID.randomUUID())
                        .decision(ClaimStatus.REJECTED).rejectionReason("Not covered")
                        .reviewedBy(UUID.randomUUID())
                        .build());

        Notification notification = awaitNotification(userId, NotificationType.CLAIM_REJECTED);

        assertThat(notification.getSubject()).isEqualTo("Claim Rejected");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void fraudDetected_createsFraudAlertNotification_whenRiskScoreAboveThreshold() {
        UUID userId = UUID.randomUUID();
        seedContact(userId);
        kafkaTemplate.send(KafkaTopics.FRAUD_DETECTED, userId.toString(),
                FraudDetectedEvent.builder()
                        .claimId(UUID.randomUUID()).userId(userId).riskScore(80)
                        .flags(List.of("AMOUNT_ABOVE_TYPE_THRESHOLD")).aiExplanation("High risk claim")
                        .build());

        Notification notification = awaitNotification(userId, NotificationType.FRAUD_ALERT);

        assertThat(notification.getSubject()).isEqualTo("Claim Flagged for Review");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }
}
