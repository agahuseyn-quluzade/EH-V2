package com.ehi.policy.kafka;

import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.event.PaymentCompletedEvent;
import com.ehi.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedEventConsumer {

    private final PolicyService policyService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "policy-service")
    public void consume(PaymentCompletedEvent event) {
        if (event.referenceType() != PaymentReferenceType.POLICY_PREMIUM) {
            return;
        }

        log.info("Activating policy {} after payment {}", event.referenceId(), event.paymentId());
        policyService.activatePolicy(event.referenceId());
    }
}
