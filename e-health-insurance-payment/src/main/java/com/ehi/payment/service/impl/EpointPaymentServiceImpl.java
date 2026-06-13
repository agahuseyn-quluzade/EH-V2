package com.ehi.payment.service.impl;

import com.ehi.payment.client.EpointCheckoutResponse;
import com.ehi.payment.client.EpointClient;
import com.ehi.payment.client.EpointPaymentResult;
import com.ehi.payment.client.EpointSignature;
import com.ehi.payment.config.EpointProperties;
import com.ehi.payment.dto.response.CardRegistrationResponse;
import com.ehi.payment.dto.response.PaymentDto;
import com.ehi.payment.dto.response.SavedCardDto;
import com.ehi.payment.entity.Payment;
import com.ehi.payment.entity.SavedCard;
import com.ehi.payment.exception.PaymentErrorEnum;
import com.ehi.payment.kafka.PaymentCompletedEventProducer;
import com.ehi.payment.kafka.PaymentFailedEventProducer;
import com.ehi.payment.mapper.PaymentMapper;
import com.ehi.payment.mapper.SavedCardMapper;
import com.ehi.payment.repository.PaymentRepository;
import com.ehi.payment.repository.SavedCardRepository;
import com.ehi.payment.service.EpointPaymentService;
import com.ehi.infra.enums.PaymentStatus;
import com.ehi.infra.event.PaymentCompletedEvent;
import com.ehi.infra.event.PaymentFailedEvent;
import com.ehi.infra.exception.NotFoundException;
import com.ehi.infra.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpointPaymentServiceImpl implements EpointPaymentService {

    private final PaymentRepository paymentRepository;
    private final SavedCardRepository savedCardRepository;
    private final EpointProperties epointProperties;
    private final EpointClient epointClient;
    private final PaymentMapper paymentMapper;
    private final SavedCardMapper savedCardMapper;
    private final ObjectMapper objectMapper;
    private final PaymentCompletedEventProducer paymentCompletedEventProducer;
    private final PaymentFailedEventProducer paymentFailedEventProducer;

    @Override
    public void handleCallback(String data, String signature) {
        if (!EpointSignature.verify(epointProperties.getPrivateKey(), data, signature)) {
            log.warn("Epoint callback rejected: invalid signature");
            throw new ServiceException(PaymentErrorEnum.EPOINT_INVALID_SIGNATURE);
        }

        EpointPaymentResult result = decode(data);

        if (result.orderId() != null) {
            applyPaymentResult(result);
        } else if (result.cardId() != null) {
            applyCardRegistrationResult(result);
        } else {
            log.warn("Epoint callback without order_id or card_id ignored: status={}", result.status());
        }
    }

    @Override
    public PaymentDto refreshStatus(UUID paymentId, UUID requesterId, boolean privileged) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment", paymentId));

        if (!privileged && !payment.getUserId().equals(requesterId)) {
            throw new NotFoundException("Payment", paymentId);
        }

        if (payment.getEpointTransaction() == null) {
            throw new ServiceException(PaymentErrorEnum.EPOINT_NO_TRANSACTION);
        }

        EpointPaymentResult result = epointClient.getStatus(payment.getEpointTransaction());
        applyResult(payment, result);

        return paymentMapper.toDto(payment);
    }

    @Override
    public CardRegistrationResponse startCardRegistration(UUID userId) {
        EpointCheckoutResponse response = epointClient.registerPayoutCard("Payout card registration");

        if (!response.isSuccess() || response.cardId() == null || response.redirectUrl() == null) {
            log.warn("Epoint card registration failed for user {}: {}", userId, response.message());
            throw new ServiceException(PaymentErrorEnum.EPOINT_CARD_REGISTRATION_FAILED);
        }

        SavedCard card = savedCardRepository.save(SavedCard.builder()
                .userId(userId)
                .cardId(response.cardId())
                .active(false)
                .build());
        log.info("Payout card registration started for user {}: card={}", userId, card.getId());

        return new CardRegistrationResponse(card.getId(), response.redirectUrl());
    }

    @Override
    public List<SavedCardDto> getMyCards(UUID userId) {
        return savedCardRepository.findByUserId(userId).stream()
                .map(savedCardMapper::toDto)
                .toList();
    }

    private void applyPaymentResult(EpointPaymentResult result) {
        UUID paymentId;
        try {
            paymentId = UUID.fromString(result.orderId());
        } catch (IllegalArgumentException e) {
            log.warn("Epoint callback with unknown order_id format ignored: {}", result.orderId());
            return;
        }

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("Epoint callback for unknown payment {} ignored", paymentId);
            return;
        }

        applyResult(payment, result);
    }

    private void applyResult(Payment payment, EpointPaymentResult result) {
        String status = result.status() == null ? "" : result.status().toLowerCase();

        switch (status) {
            case "success" -> complete(payment, result);
            case "failed", "error" -> fail(payment, result);
            case "returned" -> refund(payment);
            default -> log.info("Epoint status '{}' for payment {} — no state change", status, payment.getId());
        }
    }

    private void complete(Payment payment, EpointPaymentResult result) {
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.warn("Duplicate Epoint success for payment {} skipped", payment.getId());
            return;
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(result.transaction());
        payment.setEpointTransaction(result.transaction());
        payment.setBankTransaction(result.bankTransaction());
        payment.setRrn(result.rrn());
        payment.setCardMask(result.cardMask());
        payment.setFailureReason(null);
        paymentRepository.save(payment);
        log.info("Payment {} completed via Epoint: transaction={}, rrn={}",
                payment.getId(), result.transaction(), result.rrn());

        paymentCompletedEventProducer.publish(PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .userId(payment.getUserId())
                .referenceId(payment.getReferenceId())
                .referenceType(payment.getReferenceType())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .build());
    }

    private void fail(Payment payment, EpointPaymentResult result) {
        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.FAILED) {
            log.warn("Epoint failure for payment {} in status {} skipped", payment.getId(), payment.getStatus());
            return;
        }

        String reason = result.message() != null ? result.message() : "Epoint payment failed (code=" + result.code() + ")";
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason.length() > 255 ? reason.substring(0, 255) : reason);
        payment.setBankTransaction(result.bankTransaction());
        paymentRepository.save(payment);
        log.warn("Payment {} failed via Epoint: code={}, message={}", payment.getId(), result.code(), result.message());

        paymentFailedEventProducer.publish(PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .userId(payment.getUserId())
                .referenceId(payment.getReferenceId())
                .referenceType(payment.getReferenceType())
                .amount(payment.getAmount())
                .reason(payment.getFailureReason())
                .build());
    }

    private void refund(Payment payment) {
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        log.info("Payment {} marked REFUNDED via Epoint", payment.getId());
    }

    private void applyCardRegistrationResult(EpointPaymentResult result) {
        SavedCard card = savedCardRepository.findByCardId(result.cardId()).orElse(null);
        if (card == null) {
            log.warn("Epoint card registration callback for unknown card_id {} ignored", result.cardId());
            return;
        }

        if (!result.isSuccess()) {
            log.warn("Epoint card registration failed for card {}: {}", card.getId(), result.message());
            return;
        }

        card.setActive(true);
        card.setCardMask(result.cardMask());
        card.setCardName(result.cardName());
        savedCardRepository.save(card);
        log.info("Payout card {} activated for user {}", card.getId(), card.getUserId());
    }

    private EpointPaymentResult decode(String data) {
        try {
            return objectMapper.readValue(Base64.getMimeDecoder().decode(data), EpointPaymentResult.class);
        } catch (Exception e) {
            log.warn("Failed to decode Epoint callback data", e);
            throw new ServiceException(PaymentErrorEnum.EPOINT_INVALID_PAYLOAD);
        }
    }
}
