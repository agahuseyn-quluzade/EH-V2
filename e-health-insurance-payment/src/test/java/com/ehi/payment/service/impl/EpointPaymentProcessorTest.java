package com.ehi.payment.service.impl;

import com.ehi.payment.client.EpointCheckoutResponse;
import com.ehi.payment.client.EpointClient;
import com.ehi.payment.client.EpointPayoutResponse;
import com.ehi.payment.entity.Payment;
import com.ehi.payment.entity.SavedCard;
import com.ehi.payment.kafka.PaymentCompletedEventProducer;
import com.ehi.payment.kafka.PaymentFailedEventProducer;
import com.ehi.payment.repository.PaymentRepository;
import com.ehi.payment.repository.SavedCardRepository;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import com.ehi.infra.event.PaymentCompletedEvent;
import com.ehi.infra.event.PaymentFailedEvent;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpointPaymentProcessorTest {

    @Mock PaymentRepository paymentRepository;
    @Mock SavedCardRepository savedCardRepository;
    @Mock EpointClient epointClient;
    @Mock PaymentCompletedEventProducer paymentCompletedEventProducer;
    @Mock PaymentFailedEventProducer paymentFailedEventProducer;

    @InjectMocks EpointPaymentProcessor processor;

    private Payment pendingPayment(PaymentReferenceType referenceType) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .referenceId(UUID.randomUUID())
                .referenceType(referenceType)
                .amount(BigDecimal.valueOf(150))
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void process_unknownPayment_isNoOp() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        processor.process(paymentId);

        verify(paymentRepository, never()).save(any());
        verify(paymentFailedEventProducer, never()).publish(any(PaymentFailedEvent.class));
    }

    @Test
    void process_policyPremium_storesCheckoutUrl_andStaysPending() {
        Payment payment = pendingPayment(PaymentReferenceType.POLICY_PREMIUM);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(epointClient.createPayment(anyString(), any(), anyString())).thenReturn(
                EpointCheckoutResponse.builder()
                        .status("success")
                        .transaction("tw000123")
                        .redirectUrl("https://epoint.az/checkout/tw000123")
                        .build());

        processor.process(payment.getId());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getEpointTransaction()).isEqualTo("tw000123");
        assertThat(saved.getCheckoutUrl()).isEqualTo("https://epoint.az/checkout/tw000123");

        verify(paymentCompletedEventProducer, never()).publish(any(PaymentCompletedEvent.class));
        verify(paymentFailedEventProducer, never()).publish(any(PaymentFailedEvent.class));
    }

    @Test
    void process_policyPremium_failsPayment_whenEpointRejects() {
        Payment payment = pendingPayment(PaymentReferenceType.POLICY_PREMIUM);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(epointClient.createPayment(anyString(), any(), anyString())).thenReturn(
                EpointCheckoutResponse.builder().status("error").message("Invalid merchant").build());

        processor.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Invalid merchant");

        ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(paymentFailedEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().paymentId()).isEqualTo(payment.getId());
    }

    @Test
    void process_policyPremium_failsPayment_whenEpointUnreachable() {
        Payment payment = pendingPayment(PaymentReferenceType.POLICY_PREMIUM);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(epointClient.createPayment(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("connection refused"));

        processor.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentFailedEventProducer).publish(any(PaymentFailedEvent.class));
    }

    @Test
    void process_claimPayout_failsPayment_whenNoActiveCard() {
        Payment payment = pendingPayment(PaymentReferenceType.CLAIM_PAYOUT);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(savedCardRepository.findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(payment.getUserId()))
                .thenReturn(Optional.empty());

        processor.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("No active payout card registered");
        verify(paymentFailedEventProducer).publish(any(PaymentFailedEvent.class));
        verify(epointClient, never()).payout(anyString(), anyString(), any(), anyString());
    }

    @Test
    void process_claimPayout_completesPayment_andPublishesEvent() {
        Payment payment = pendingPayment(PaymentReferenceType.CLAIM_PAYOUT);
        SavedCard card = SavedCard.builder()
                .id(UUID.randomUUID()).userId(payment.getUserId()).cardId("ce123").active(true).build();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(savedCardRepository.findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(payment.getUserId()))
                .thenReturn(Optional.of(card));
        when(epointClient.payout(anyString(), anyString(), any(), anyString())).thenReturn(
                EpointPayoutResponse.builder()
                        .status("success")
                        .transaction("tw000456")
                        .bankTransaction("bank-1")
                        .rrn("rrn-1")
                        .cardMask("123456******1234")
                        .build());

        processor.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getTransactionId()).isEqualTo("tw000456");
        assertThat(payment.getBankTransaction()).isEqualTo("bank-1");
        assertThat(payment.getRrn()).isEqualTo("rrn-1");
        assertThat(payment.getCardMask()).isEqualTo("123456******1234");

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentCompletedEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().paymentId()).isEqualTo(payment.getId());
        assertThat(eventCaptor.getValue().transactionId()).isEqualTo("tw000456");
    }

    @Test
    void process_claimPayout_failsPayment_whenPayoutRejected() {
        Payment payment = pendingPayment(PaymentReferenceType.CLAIM_PAYOUT);
        SavedCard card = SavedCard.builder()
                .id(UUID.randomUUID()).userId(payment.getUserId()).cardId("ce123").active(true).build();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(savedCardRepository.findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(payment.getUserId()))
                .thenReturn(Optional.of(card));
        when(epointClient.payout(anyString(), anyString(), any(), anyString())).thenReturn(
                EpointPayoutResponse.builder().status("failed").message("Insufficient funds").build());

        processor.process(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Insufficient funds");
        verify(paymentCompletedEventProducer, never()).publish(any(PaymentCompletedEvent.class));
        verify(paymentFailedEventProducer).publish(any(PaymentFailedEvent.class));
    }
}
