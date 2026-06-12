package com.ehi.payment.service.impl;

import com.ehi.payment.entity.Payment;
import com.ehi.payment.kafka.PaymentCompletedEventProducer;
import com.ehi.payment.repository.PaymentRepository;
import com.ehi.infra.enums.PaymentStatus;
import com.ehi.infra.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MockPaymentProcessor {

    private static final long PROCESSING_DELAY_MS = 2000;

    private final PaymentRepository paymentRepository;
    private final PaymentCompletedEventProducer paymentCompletedEventProducer;

    @Async
    public void process(UUID paymentId) {
        try {
            Thread.sleep(PROCESSING_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("Payment {} not found when completing mock processing", paymentId);
            return;
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId("TXN-" + UUID.randomUUID());
        paymentRepository.save(payment);

        paymentCompletedEventProducer.publish(PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .userId(payment.getUserId())
                .referenceId(payment.getReferenceId())
                .referenceType(payment.getReferenceType())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .build());
    }
}
