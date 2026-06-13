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
import com.ehi.payment.service.PaymentProcessor;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import com.ehi.infra.event.PaymentCompletedEvent;
import com.ehi.infra.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.provider", havingValue = "epoint")
public class EpointPaymentProcessor implements PaymentProcessor {

    private final PaymentRepository paymentRepository;
    private final SavedCardRepository savedCardRepository;
    private final EpointClient epointClient;
    private final PaymentCompletedEventProducer paymentCompletedEventProducer;
    private final PaymentFailedEventProducer paymentFailedEventProducer;

    @Override
    public void process(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("Payment {} not found for Epoint processing", paymentId);
            return;
        }

        try {
            if (payment.getReferenceType() == PaymentReferenceType.CLAIM_PAYOUT) {
                payout(payment);
            } else {
                createCheckout(payment);
            }
        } catch (Exception e) {
            log.warn("Epoint processing failed for payment {}", paymentId, e);
            fail(payment, "Epoint error: " + e.getMessage());
        }
    }

    private void createCheckout(Payment payment) {
        EpointCheckoutResponse response = epointClient.createPayment(
                payment.getId().toString(), payment.getAmount(),
                "Policy premium " + payment.getReferenceId());

        if (!response.isSuccess() || response.redirectUrl() == null) {
            fail(payment, response.message() != null ? response.message() : "Epoint checkout creation failed");
            return;
        }

        payment.setEpointTransaction(response.transaction());
        payment.setCheckoutUrl(response.redirectUrl());
        paymentRepository.save(payment);
        log.info("Epoint checkout created for payment {}: transaction={}", payment.getId(), response.transaction());
    }

    private void payout(Payment payment) {
        Optional<SavedCard> card = savedCardRepository
                .findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(payment.getUserId());
        if (card.isEmpty()) {
            fail(payment, "No active payout card registered");
            return;
        }

        EpointPayoutResponse response = epointClient.payout(
                card.get().getCardId(), payment.getId().toString(), payment.getAmount(),
                "Claim payout " + payment.getReferenceId());

        if (!response.isSuccess()) {
            fail(payment, response.message() != null ? response.message() : "Epoint payout failed");
            return;
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(response.transaction());
        payment.setEpointTransaction(response.transaction());
        payment.setBankTransaction(response.bankTransaction());
        payment.setRrn(response.rrn());
        payment.setCardMask(response.cardMask());
        paymentRepository.save(payment);
        log.info("Epoint payout completed for payment {}: transaction={}", payment.getId(), response.transaction());

        paymentCompletedEventProducer.publish(PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .userId(payment.getUserId())
                .referenceId(payment.getReferenceId())
                .referenceType(payment.getReferenceType())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .build());
    }

    private void fail(Payment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
        paymentRepository.save(payment);
        log.warn("Payment {} failed: {}", payment.getId(), reason);

        paymentFailedEventProducer.publish(PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .userId(payment.getUserId())
                .referenceId(payment.getReferenceId())
                .referenceType(payment.getReferenceType())
                .amount(payment.getAmount())
                .reason(payment.getFailureReason())
                .build());
    }
}
