package com.ehi.payment.kafka;

import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFailedEventProducer {

    private final KafkaTemplate<String, PaymentFailedEvent> kafkaTemplate;

    public void publish(PaymentFailedEvent event) {
        kafkaTemplate.send(KafkaTopics.PAYMENT_FAILED, event.paymentId().toString(), event);
        log.info("Published PaymentFailedEvent for paymentId={}, referenceId={}, reason={}", event.paymentId(), event.referenceId(), event.reason());
    }
}
