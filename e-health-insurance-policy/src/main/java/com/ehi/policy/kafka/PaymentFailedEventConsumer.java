package com.ehi.policy.kafka;

import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.event.PaymentFailedEvent;
import com.ehi.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFailedEventConsumer {

    private final PolicyService policyService;

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "policy-service")
    public void consume(PaymentFailedEvent event) {
        if (event.referenceType() != PaymentReferenceType.POLICY_PREMIUM) {
            return;
        }

        log.info("Cancelling policy {} after failed payment {}", event.referenceId(), event.paymentId());
        policyService.cancelPolicyOnPaymentFailure(event.referenceId());
    }
}
