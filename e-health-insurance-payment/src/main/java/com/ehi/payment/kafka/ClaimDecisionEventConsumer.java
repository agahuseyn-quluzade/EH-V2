package com.ehi.payment.kafka;

import com.ehi.payment.service.PaymentService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.ClaimStatus;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.event.ClaimDecisionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimDecisionEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = KafkaTopics.CLAIM_DECISION, groupId = "payment-service")
    public void consume(ClaimDecisionEvent event) {
        if (event.decision() != ClaimStatus.APPROVED) {
            return;
        }

        log.info("Received approved ClaimDecisionEvent for claimId={}, userId={}", event.claimId(), event.userId());
        paymentService.processPayment(event.userId(), event.claimId(), PaymentReferenceType.CLAIM_PAYOUT, event.approvedAmount());
    }
}
