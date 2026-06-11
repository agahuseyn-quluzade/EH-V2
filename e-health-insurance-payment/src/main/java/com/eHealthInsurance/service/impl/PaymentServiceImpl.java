package com.eHealthInsurance.service.impl;

import com.eHealthInsurance.client.IamServiceClient;
import com.eHealthInsurance.client.PolicyServiceClient;
import com.eHealthInsurance.client.dto.MemberStatusResponse;
import com.eHealthInsurance.client.dto.PolicyResponse;
import com.eHealthInsurance.common.events.payment.PaymentFailedEvent;
import com.eHealthInsurance.common.events.payment.PaymentInitiatedEvent;
import com.eHealthInsurance.common.events.payment.PaymentRefundedEvent;
import com.eHealthInsurance.common.events.payment.PaymentSucceededEvent;
import com.eHealthInsurance.dto.request.ConfirmPaymentRequest;
import com.eHealthInsurance.dto.request.CreatePaymentRequest;
import com.eHealthInsurance.dto.request.CreateRefundRequest;
import com.eHealthInsurance.dto.request.FailPaymentRequest;
import com.eHealthInsurance.dto.response.InvoiceResponse;
import com.eHealthInsurance.dto.response.PaymentResponse;
import com.eHealthInsurance.dto.response.RefundResponse;
import com.eHealthInsurance.entity.Payment;
import com.eHealthInsurance.entity.PaymentAttempt;
import com.eHealthInsurance.entity.Refund;
import com.eHealthInsurance.entity.enums.PaymentAttemptStatus;
import com.eHealthInsurance.entity.enums.PaymentStatus;
import com.eHealthInsurance.entity.enums.RefundStatus;
import com.eHealthInsurance.exception.PaymentErrorEnum;
import com.eHealthInsurance.exception.PaymentException;
import com.eHealthInsurance.mapper.PaymentMapper;
import com.eHealthInsurance.outbox.EventMetadata;
import com.eHealthInsurance.outbox.EventMetadataFactory;
import com.eHealthInsurance.outbox.OutboxService;
import com.eHealthInsurance.repository.InvoiceRepository;
import com.eHealthInsurance.repository.PaymentAttemptRepository;
import com.eHealthInsurance.repository.PaymentRepository;
import com.eHealthInsurance.repository.RefundRepository;
import com.eHealthInsurance.service.PaymentService;
import com.eHealthInsurance.service.idempotency.PaymentIdempotencyService;
import com.eHealthInsurance.service.provider.PaymentProviderClient;
import com.eHealthInsurance.service.provider.PaymentProviderRegistry;
import com.eHealthInsurance.service.provider.ProviderFailureResult;
import com.eHealthInsurance.service.provider.ProviderPaymentResult;
import com.eHealthInsurance.service.provider.ProviderRefundResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final RefundRepository refundRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentMapper paymentMapper;
    private final PolicyServiceClient policyServiceClient;
    private final IamServiceClient iamServiceClient;
    private final OutboxService outboxService;
    private final EventMetadataFactory eventMetadataFactory;
    private final PaymentIdempotencyService idempotencyService;
    private final PaymentProviderRegistry providerRegistry;

    @Override
    @Transactional
    public PaymentResponse createPayment(UUID memberId, CreatePaymentRequest request) {
        return createPayment(memberId, request, null);
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(UUID memberId, CreatePaymentRequest request, String idempotencyKeyHeader) {
        String idempotencyKey = resolveIdempotencyKey(idempotencyKeyHeader, request.idempotencyKey());

        Payment existing = findExistingPayment(memberId, idempotencyKey);
        if (existing != null) {
            return paymentMapper.toResponse(existing);
        }

        boolean lockAcquired = idempotencyService.acquireLock(memberId, idempotencyKey);
        if (!lockAcquired) {
            existing = findExistingPayment(memberId, idempotencyKey);
            if (existing != null) {
                return paymentMapper.toResponse(existing);
            }
            throw new PaymentException(PaymentErrorEnum.IDEMPOTENCY_REQUEST_IN_PROGRESS);
        }

        try {
            validatePolicy(request.policyId());
            validateActiveMember(memberId);
            validatePositiveAmount(request.amount());

            Payment payment = Payment.builder()
                    .policyId(request.policyId())
                    .memberId(memberId)
                    .amount(request.amount())
                    .provider(request.provider())
                    .status(PaymentStatus.PROCESSING)
                    .idempotencyKey(idempotencyKey)
                    .build();
            payment.prePersist();

            PaymentProviderClient provider = providerRegistry.get(request.provider());
            ProviderPaymentResult providerResult = provider.initiate(payment, request.paymentMethodToken());
            payment.setProviderReference(providerResult.providerReference());

            payment = paymentRepository.save(payment);
            paymentAttemptRepository.save(attempt(
                    payment,
                    PaymentAttemptStatus.INITIATED,
                    providerResult.providerReference(),
                    null,
                    null,
                    idempotencyKey,
                    null
            ));
            enqueuePaymentInitiated(payment);
            idempotencyService.rememberPayment(memberId, idempotencyKey, payment.getId());
            return paymentMapper.toResponse(payment);
        } finally {
            idempotencyService.releaseLock(memberId, idempotencyKey);
        }
    }

    @Override
    public PaymentResponse getPayment(UUID paymentId, UUID requesterId, String role) {
        Payment payment;
        if ("MEMBER".equalsIgnoreCase(role)) {
            payment = paymentRepository.findByIdAndMemberId(paymentId, requesterId)
                    .orElseThrow(() -> new PaymentException(PaymentErrorEnum.PAYMENT_NOT_FOUND));
        } else {
            payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentException(PaymentErrorEnum.PAYMENT_NOT_FOUND));
        }
        return paymentMapper.toResponse(payment);
    }

    @Override
    public List<PaymentResponse> getMemberPayments(UUID memberId) {
        return paymentRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(UUID paymentId, ConfirmPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorEnum.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentException(PaymentErrorEnum.PAYMENT_ALREADY_CONFIRMED);
        }
        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentException(PaymentErrorEnum.PAYMENT_INVALID_STATE);
        }

        PaymentProviderClient provider = providerRegistry.get(payment.getProvider());
        ProviderPaymentResult providerResult = provider.confirm(payment, request.providerReference());

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(Instant.now());
        payment.setProviderReference(providerResult.providerReference());
        payment.setFailureCode(null);
        payment.setFailureReason(null);
        payment.setFailedAt(null);
        payment = paymentRepository.save(payment);

        paymentAttemptRepository.save(attempt(
                payment,
                PaymentAttemptStatus.SUCCEEDED,
                providerResult.providerReference(),
                null,
                null,
                payment.getIdempotencyKey(),
                payment.getPaidAt()
        ));
        enqueuePaymentSucceeded(payment);
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse failPayment(UUID paymentId, FailPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorEnum.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.COMPLETED || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentException(PaymentErrorEnum.PAYMENT_INVALID_STATE);
        }

        PaymentProviderClient provider = providerRegistry.get(payment.getProvider());
        ProviderFailureResult failure = provider.fail(
                payment,
                request.providerReference(),
                request.failureCode(),
                request.failureReason()
        );

        payment.setStatus(PaymentStatus.FAILED);
        payment.setProviderReference(failure.providerReference());
        payment.setFailureCode(failure.failureCode());
        payment.setFailureReason(failure.failureReason());
        payment.setFailedAt(Instant.now());
        payment = paymentRepository.save(payment);

        paymentAttemptRepository.save(attempt(
                payment,
                PaymentAttemptStatus.FAILED,
                failure.providerReference(),
                failure.failureCode(),
                failure.failureReason(),
                payment.getIdempotencyKey(),
                payment.getFailedAt()
        ));
        enqueuePaymentFailed(payment);
        return paymentMapper.toResponse(payment);
    }

    @Override
    public List<InvoiceResponse> getInvoicesByPolicyId(UUID policyId) {
        return invoiceRepository.findByPolicyIdOrderByCreatedAtDesc(policyId).stream()
                .map(paymentMapper::toInvoiceResponse)
                .toList();
    }

    @Override
    @Transactional
    public RefundResponse createRefund(UUID memberId, CreateRefundRequest request) {
        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new PaymentException(PaymentErrorEnum.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentException(PaymentErrorEnum.PAYMENT_INVALID_STATE);
        }
        validatePositiveAmount(request.amount());
        if (request.amount().compareTo(payment.getAmount()) > 0) {
            throw new PaymentException(PaymentErrorEnum.PAYMENT_INVALID_AMOUNT);
        }

        Refund refund = Refund.builder()
                .paymentId(request.paymentId())
                .memberId(memberId)
                .amount(request.amount())
                .reason(request.reason())
                .requestedBy(memberId)
                .status(RefundStatus.PROCESSED)
                .processedAt(Instant.now())
                .build();
        refund.prePersist();

        ProviderRefundResult providerResult = providerRegistry.get(payment.getProvider()).refund(payment, refund);
        refund.setProviderReference(providerResult.refundReference());
        refund = refundRepository.save(refund);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        enqueuePaymentRefunded(payment, refund);
        return paymentMapper.toRefundResponse(refund);
    }

    @Override
    public List<RefundResponse> getMemberRefunds(UUID memberId) {
        return refundRepository.findByMemberIdOrderByRequestedAtDesc(memberId).stream()
                .map(paymentMapper::toRefundResponse)
                .toList();
    }

    private Payment findExistingPayment(UUID memberId, String idempotencyKey) {
        return idempotencyService.findPaymentId(memberId, idempotencyKey)
                .flatMap(paymentRepository::findById)
                .or(() -> paymentRepository.findByMemberIdAndIdempotencyKey(memberId, idempotencyKey))
                .orElse(null);
    }

    private String resolveIdempotencyKey(String headerValue, String requestValue) {
        String candidate = hasText(headerValue) ? headerValue : requestValue;
        if (!hasText(candidate)) {
            throw new PaymentException(PaymentErrorEnum.IDEMPOTENCY_KEY_REQUIRED);
        }
        String normalized = candidate.trim();
        if (normalized.length() > 120) {
            throw new PaymentException(PaymentErrorEnum.IDEMPOTENCY_KEY_REQUIRED);
        }
        return normalized;
    }

    private void validatePolicy(UUID policyId) {
        try {
            PolicyResponse policy = policyServiceClient.getPolicy(policyId);
            if (policy == null) {
                throw new PaymentException(PaymentErrorEnum.POLICY_NOT_FOUND);
            }
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException(PaymentErrorEnum.POLICY_NOT_FOUND, e);
        }
    }

    private void validateActiveMember(UUID memberId) {
        try {
            MemberStatusResponse member = iamServiceClient.getMemberStatus(memberId);
            if (member == null || !member.active()) {
                throw new PaymentException(PaymentErrorEnum.MEMBER_NOT_FOUND);
            }
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException(PaymentErrorEnum.MEMBER_NOT_FOUND, e);
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new PaymentException(PaymentErrorEnum.PAYMENT_INVALID_AMOUNT);
        }
    }

    private PaymentAttempt attempt(
            Payment payment,
            PaymentAttemptStatus status,
            String providerReference,
            String failureCode,
            String failureReason,
            String idempotencyKey,
            Instant completedAt
    ) {
        return PaymentAttempt.builder()
                .payment(payment)
                .provider(payment.getProvider())
                .status(status)
                .providerReference(providerReference)
                .failureCode(failureCode)
                .failureReason(failureReason)
                .idempotencyKey(idempotencyKey)
                .completedAt(completedAt)
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void enqueuePaymentInitiated(Payment payment) {
        EventMetadata metadata = eventMetadataFactory.create(payment.getId(), payment.getMemberId());
        outboxService.enqueue(new PaymentInitiatedEvent(
                metadata.schemaVersion(),
                metadata.eventId(),
                metadata.occurredAt(),
                metadata.correlationId(),
                metadata.causationId(),
                metadata.producer(),
                metadata.aggregateId(),
                metadata.userId(),
                payment.getId(),
                payment.getPolicyId(),
                payment.getMemberId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider().name(),
                payment.getStatus().name()
        ));
    }

    private void enqueuePaymentSucceeded(Payment payment) {
        EventMetadata metadata = eventMetadataFactory.create(payment.getId(), payment.getMemberId());
        outboxService.enqueue(new PaymentSucceededEvent(
                metadata.schemaVersion(),
                metadata.eventId(),
                metadata.occurredAt(),
                metadata.correlationId(),
                metadata.causationId(),
                metadata.producer(),
                metadata.aggregateId(),
                metadata.userId(),
                payment.getId(),
                payment.getPolicyId(),
                payment.getMemberId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider().name(),
                payment.getProviderReference(),
                payment.getPaidAt()
        ));
    }

    private void enqueuePaymentFailed(Payment payment) {
        EventMetadata metadata = eventMetadataFactory.create(payment.getId(), payment.getMemberId());
        outboxService.enqueue(new PaymentFailedEvent(
                metadata.schemaVersion(),
                metadata.eventId(),
                metadata.occurredAt(),
                metadata.correlationId(),
                metadata.causationId(),
                metadata.producer(),
                metadata.aggregateId(),
                metadata.userId(),
                payment.getId(),
                payment.getPolicyId(),
                payment.getMemberId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider().name(),
                payment.getFailureCode(),
                payment.getFailureReason()
        ));
    }

    private void enqueuePaymentRefunded(Payment payment, Refund refund) {
        EventMetadata metadata = eventMetadataFactory.create(payment.getId(), refund.getMemberId());
        outboxService.enqueue(new PaymentRefundedEvent(
                metadata.schemaVersion(),
                metadata.eventId(),
                metadata.occurredAt(),
                metadata.correlationId(),
                metadata.causationId(),
                metadata.producer(),
                metadata.aggregateId(),
                metadata.userId(),
                payment.getId(),
                payment.getPolicyId(),
                refund.getMemberId(),
                refund.getAmount(),
                payment.getCurrency(),
                payment.getProviderReference(),
                refund.getProviderReference(),
                refund.getProcessedAt()
        ));
    }
}
