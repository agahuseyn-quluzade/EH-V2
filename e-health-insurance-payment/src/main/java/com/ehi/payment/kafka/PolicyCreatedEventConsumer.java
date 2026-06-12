package com.ehi.payment.kafka;

import com.ehi.payment.service.PaymentService;
import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.event.PolicyCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyCreatedEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = KafkaTopics.POLICY_CREATED, groupId = "payment-service")
    public void consume(PolicyCreatedEvent event) {
        log.info("Received PolicyCreatedEvent for policyId={}, userId={}", event.policyId(), event.userId());
        paymentService.processPayment(event.userId(), event.policyId(), PaymentReferenceType.POLICY_PREMIUM, event.premiumAmount());
    }
}
