package com.ehi.payment.service.impl;

import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import com.ehi.infra.event.PaymentCompletedEvent;
import com.ehi.infra.event.PaymentFailedEvent;
import com.ehi.infra.exception.BadRequestException;
import com.ehi.infra.exception.NotFoundException;
import com.ehi.payment.client.EpointClient;
import com.ehi.payment.client.EpointClientResponse;
import com.ehi.payment.config.EpointProperties;
import com.ehi.payment.dto.request.EpointInitPaymentRequest;
import com.ehi.payment.dto.request.EpointReverseRequest;
import com.ehi.payment.dto.response.EpointPaymentResponse;
import com.ehi.payment.entity.Payment;
import com.ehi.payment.entity.PaymentTransaction;
import com.ehi.payment.kafka.PaymentCompletedEventProducer;
import com.ehi.payment.kafka.PaymentFailedEventProducer;
import com.ehi.payment.repository.PaymentRepository;
import com.ehi.payment.repository.PaymentTransactionRepository;
import com.ehi.payment.service.EpointPaymentService;
import com.ehi.payment.service.EpointSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpointPaymentServiceImpl implements EpointPaymentService {

    private static final String PROVIDER = "EPOINT";

    private final EpointClient epointClient;
    private final EpointProperties epointProperties;
    private final EpointSignatureService signatureService;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentCompletedEventProducer paymentCompletedEventProducer;
    private final PaymentFailedEventProducer paymentFailedEventProducer;

    @Override
    @Transactional
    public EpointPaymentResponse initPayment(UUID userId, EpointInitPaymentRequest request) {
        PaymentReferenceType referenceType = request.referenceType() == null
                ? PaymentReferenceType.POLICY_PREMIUM
                : request.referenceType();

        PaymentTransaction existing = transactionRepository
                .findFirstByReferenceIdAndReferenceTypeAndStatusNotOrderByCreatedAtDesc(
                        request.referenceId(), referenceType, PaymentStatus.FAILED)
                .orElse(null);
        if (existing != null) {
            if (!existing.getUserId().equals(userId)) {
                throw new NotFoundException("PaymentTransaction", request.referenceId());
            }
            return toResponse(existing);
        }

        Payment payment = paymentRepository.save(Payment.builder()
                .userId(userId)
                .referenceId(request.referenceId())
                .referenceType(referenceType)
                .amount(request.amount())
                .status(PaymentStatus.PENDING)
                .build());

        PaymentTransaction transaction = transactionRepository.save(PaymentTransaction.builder()
                .paymentId(payment.getId())
                .userId(userId)
                .referenceId(request.referenceId())
                .referenceType(referenceType)
                .amount(request.amount())
                .currency(epointProperties.getCurrency())
                .provider(PROVIDER)
                .orderId(generateOrderId())
                .status(PaymentStatus.PENDING)
                .build());

        EpointClientResponse response;
        try {
            response = epointClient.requestPayment(
                    transaction.getOrderId(), transaction.getAmount(), description(request, transaction));
        } catch (RuntimeException ex) {
            log.warn("Epoint payment initialization failed for orderId={}", transaction.getOrderId(), ex);
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureReason("Epoint payment initialization failed");
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(transaction.getFailureReason());
            paymentRepository.save(payment);
            transaction = transactionRepository.save(transaction);
            publishFailed(payment, transaction.getFailureReason());
            return toResponse(transaction);
        }
        Map<String, Object> payload = response.payload();

        transaction.setGatewayStatus(asString(payload.get("status")));
        transaction.setGatewayCode(asString(payload.get("code")));
        transaction.setRedirectUrl(asString(payload.get("redirect_url")));
        transaction.setFailureReason(firstPresent(payload, "message", "error", "description"));

        if (!isSuccess(payload) || transaction.getRedirectUrl() == null || transaction.getRedirectUrl().isBlank()) {
            transaction.setStatus(PaymentStatus.FAILED);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(transaction.getFailureReason() == null
                    ? "Epoint payment initialization failed"
                    : transaction.getFailureReason());
            paymentRepository.save(payment);
            transaction = transactionRepository.save(transaction);
            publishFailed(payment, transaction.getFailureReason());
            return toResponse(transaction);
        }

        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    @Override
    @Transactional
    public EpointPaymentResponse processCallback(String data, String signature) {
        if (!signatureService.verify(data, signature)) {
            log.warn("Rejected Epoint callback because signature is invalid");
            throw new BadRequestException("Invalid Epoint callback signature");
        }

        Map<String, Object> payload = signatureService.decodeData(data);
        return toResponse(applyGatewayResult(findTransaction(payload), payload, true));
    }

    @Override
    @Transactional(readOnly = true)
    public EpointPaymentResponse getStatus(UUID transactionId, UUID requesterId, boolean privileged) {
        return toResponse(findAccessibleTransaction(transactionId, requesterId, privileged));
    }

    @Override
    @Transactional
    public EpointPaymentResponse syncStatus(UUID transactionId, UUID requesterId, boolean privileged) {
        PaymentTransaction transaction = findAccessibleTransaction(transactionId, requesterId, privileged);
        EpointClientResponse response = transaction.getEpointTransaction() == null || transaction.getEpointTransaction().isBlank()
                ? epointClient.getStatusByOrderId(transaction.getOrderId())
                : epointClient.getStatusByTransaction(transaction.getEpointTransaction());

        return toResponse(applyGatewayResult(transaction, response.payload(), false));
    }

    @Override
    @Transactional
    public EpointPaymentResponse reverse(UUID transactionId, UUID requesterId, boolean privileged, EpointReverseRequest request) {
        PaymentTransaction transaction = findAccessibleTransaction(transactionId, requesterId, privileged);
        if (transaction.getEpointTransaction() == null || transaction.getEpointTransaction().isBlank()) {
            throw new BadRequestException("Epoint transaction is not available for reverse");
        }

        BigDecimal amount = request == null ? null : request.amount();
        EpointClientResponse response = epointClient.reverse(transaction.getEpointTransaction(), amount);
        Map<String, Object> payload = response.payload();
        transaction.setGatewayStatus(asString(payload.get("status")));
        transaction.setGatewayCode(asString(payload.get("code")));
        transaction.setOperationCode(asString(payload.get("operation_code")));
        transaction.setFailureReason(firstPresent(payload, "message", "error", "description"));

        if (isSuccess(payload)) {
            transaction.setStatus(PaymentStatus.REFUNDED);
            transaction.setReversedAt(Instant.now());
            paymentRepository.findById(transaction.getPaymentId()).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            });
        }

        return toResponse(transactionRepository.save(transaction));
    }

    private PaymentTransaction applyGatewayResult(PaymentTransaction transaction, Map<String, Object> payload, boolean callback) {
        if (callback) {
            transaction.setCallbackReceivedAt(Instant.now());
        }

        updateSafeGatewayFields(transaction, payload);

        if (transaction.getStatus() == PaymentStatus.COMPLETED
                || transaction.getStatus() == PaymentStatus.FAILED
                || transaction.getStatus() == PaymentStatus.REFUNDED) {
            return transactionRepository.save(transaction);
        }

        UUID paymentId = transaction.getPaymentId();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment", paymentId));

        if (isSuccess(payload)) {
            transaction.setStatus(PaymentStatus.COMPLETED);
            transaction.setCompletedAt(Instant.now());
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId(transaction.getEpointTransaction() == null
                    ? transaction.getOrderId()
                    : transaction.getEpointTransaction());
            paymentRepository.save(payment);
            transaction = transactionRepository.save(transaction);
            publishCompleted(payment);
            return transaction;
        }

        if (isFailure(payload)) {
            String reason = transaction.getFailureReason() == null ? "Epoint payment failed" : transaction.getFailureReason();
            transaction.setStatus(PaymentStatus.FAILED);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);
            transaction = transactionRepository.save(transaction);
            publishFailed(payment, reason);
            return transaction;
        }

        return transactionRepository.save(transaction);
    }

    private PaymentTransaction findTransaction(Map<String, Object> payload) {
        String orderId = asString(payload.get("order_id"));
        if (orderId != null && !orderId.isBlank()) {
            return transactionRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new NotFoundException("PaymentTransaction", orderId));
        }

        String transaction = asString(payload.get("transaction"));
        if (transaction != null && !transaction.isBlank()) {
            return transactionRepository.findByEpointTransaction(transaction)
                    .orElseThrow(() -> new NotFoundException("PaymentTransaction", transaction));
        }

        throw new BadRequestException("Epoint response does not contain order_id or transaction");
    }

    private PaymentTransaction findAccessibleTransaction(UUID transactionId, UUID requesterId, boolean privileged) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("PaymentTransaction", transactionId));

        if (!privileged && !transaction.getUserId().equals(requesterId)) {
            throw new NotFoundException("PaymentTransaction", transactionId);
        }

        return transaction;
    }

    private void updateSafeGatewayFields(PaymentTransaction transaction, Map<String, Object> payload) {
        transaction.setGatewayStatus(asString(payload.get("status")));
        transaction.setGatewayCode(asString(payload.get("code")));
        setIfPresent(transaction::setEpointTransaction, firstPresent(payload, "transaction", "payment_id"));
        setIfPresent(transaction::setBankTransaction, asString(payload.get("bank_transaction")));
        setIfPresent(transaction::setCardMask, asString(payload.get("card_mask")));
        setIfPresent(transaction::setCardName, asString(payload.get("card_name")));
        setIfPresent(transaction::setOperationCode, asString(payload.get("operation_code")));
        transaction.setFailureReason(firstPresent(payload, "message", "error", "description"));
    }

    private void publishCompleted(Payment payment) {
        paymentCompletedEventProducer.publish(PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .userId(payment.getUserId())
                .referenceId(payment.getReferenceId())
                .referenceType(payment.getReferenceType())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .build());
    }

    private void publishFailed(Payment payment, String reason) {
        paymentFailedEventProducer.publish(PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .userId(payment.getUserId())
                .referenceId(payment.getReferenceId())
                .referenceType(payment.getReferenceType())
                .amount(payment.getAmount())
                .reason(reason)
                .build());
    }

    private boolean isSuccess(Map<String, Object> payload) {
        String status = normalizeStatus(payload);
        return "success".equals(status) || "completed".equals(status) || "approved".equals(status);
    }

    private boolean isFailure(Map<String, Object> payload) {
        String status = normalizeStatus(payload);
        return "failed".equals(status)
                || "failure".equals(status)
                || "error".equals(status)
                || "declined".equals(status)
                || "rejected".equals(status)
                || "canceled".equals(status)
                || "cancelled".equals(status);
    }

    private String normalizeStatus(Map<String, Object> payload) {
        String status = asString(payload.get("status"));
        return status == null ? "" : status.toLowerCase(Locale.ROOT);
    }

    private String description(EpointInitPaymentRequest request, PaymentTransaction transaction) {
        if (request.description() != null && !request.description().isBlank()) {
            return request.description();
        }
        return "Insurance payment " + transaction.getOrderId();
    }

    private String generateOrderId() {
        return "EHI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase(Locale.ROOT);
    }

    private String firstPresent(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            String value = asString(payload.get(key));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void setIfPresent(java.util.function.Consumer<String> setter, String value) {
        if (value != null && !value.isBlank()) {
            setter.accept(value);
        }
    }

    private EpointPaymentResponse toResponse(PaymentTransaction transaction) {
        return EpointPaymentResponse.builder()
                .id(transaction.getId())
                .paymentId(transaction.getPaymentId())
                .userId(transaction.getUserId())
                .referenceId(transaction.getReferenceId())
                .referenceType(transaction.getReferenceType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .orderId(transaction.getOrderId())
                .status(transaction.getStatus())
                .redirectUrl(transaction.getRedirectUrl())
                .epointTransaction(transaction.getEpointTransaction())
                .bankTransaction(transaction.getBankTransaction())
                .cardMask(transaction.getCardMask())
                .cardName(transaction.getCardName())
                .gatewayStatus(transaction.getGatewayStatus())
                .gatewayCode(transaction.getGatewayCode())
                .operationCode(transaction.getOperationCode())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
