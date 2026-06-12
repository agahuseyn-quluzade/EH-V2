package com.ehi.payment.kafka;

import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedEventProducer {

    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    public void publish(PaymentCompletedEvent event) {
        kafkaTemplate.send(KafkaTopics.PAYMENT_COMPLETED, event.paymentId().toString(), event);
        log.info("Published PaymentCompletedEvent for paymentId={}, referenceId={}", event.paymentId(), event.referenceId());
    }
}
