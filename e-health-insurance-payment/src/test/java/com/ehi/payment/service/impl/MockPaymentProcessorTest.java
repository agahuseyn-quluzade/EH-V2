package com.ehi.payment.service.impl;

import com.ehi.payment.entity.Payment;
import com.ehi.payment.kafka.PaymentCompletedEventProducer;
import com.ehi.payment.repository.PaymentRepository;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import com.ehi.infra.event.PaymentCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockPaymentProcessorTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentCompletedEventProducer paymentCompletedEventProducer;

    @InjectMocks MockPaymentProcessor mockPaymentProcessor;

    @Test
    void process_completesPendingPayment_andPublishesEvent() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.POLICY_PREMIUM)
                .amount(BigDecimal.valueOf(100))
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockPaymentProcessor.process(payment.getId());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(saved.getTransactionId()).startsWith("TXN-");

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentCompletedEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().paymentId()).isEqualTo(payment.getId());
        assertThat(eventCaptor.getValue().transactionId()).startsWith("TXN-");
    }

    @Test
    void process_noOps_whenPaymentMissing() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        mockPaymentProcessor.process(paymentId);

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentCompletedEventProducer, never()).publish(any(PaymentCompletedEvent.class));
    }
}
