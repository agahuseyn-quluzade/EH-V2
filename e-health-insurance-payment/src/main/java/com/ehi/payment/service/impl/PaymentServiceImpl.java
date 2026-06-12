package com.ehi.payment.service.impl;

import com.ehi.payment.dto.response.PaymentDto;
import com.ehi.payment.entity.Payment;
import com.ehi.payment.kafka.PaymentFailedEventProducer;
import com.ehi.payment.mapper.PaymentMapper;
import com.ehi.payment.repository.PaymentRepository;
import com.ehi.payment.service.PaymentService;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import com.ehi.infra.event.PaymentFailedEvent;
import com.ehi.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentFailedEventProducer paymentFailedEventProducer;
    private final MockPaymentProcessor mockPaymentProcessor;

    @Override
    public PaymentDto processPayment(UUID userId, UUID referenceId, PaymentReferenceType referenceType, BigDecimal amount) {
        Optional<Payment> existing = paymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(
                referenceId, referenceType, PaymentStatus.FAILED);
        if (existing.isPresent()) {
            log.warn("Duplicate payment skipped for referenceId={}, type={}", referenceId, referenceType);
            return paymentMapper.toDto(existing.get());
        }

        Payment payment = Payment.builder()
                .userId(userId)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Payment failed (invalid amount): referenceId={}, amount={}", referenceId, amount);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid payment amount");
            payment = paymentRepository.save(payment);

            paymentFailedEventProducer.publish(PaymentFailedEvent.builder()
                    .paymentId(payment.getId())
                    .userId(payment.getUserId())
                    .referenceId(payment.getReferenceId())
                    .referenceType(payment.getReferenceType())
                    .amount(payment.getAmount())
                    .reason(payment.getFailureReason())
                    .build());

            return paymentMapper.toDto(payment);
        }

        payment = paymentRepository.save(payment);
        log.info("Processing payment: referenceId={}, type={}, amount={}", referenceId, referenceType, amount);
        mockPaymentProcessor.process(payment.getId());

        return paymentMapper.toDto(payment);
    }

    @Override
    public List<PaymentDto> getMyPayments(UUID userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(paymentMapper::toDto)
                .toList();
    }

    @Override
    public PaymentDto getPaymentById(UUID paymentId, UUID requesterId, boolean privileged) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment", paymentId));

        if (!privileged && !payment.getUserId().equals(requesterId)) {
            throw new NotFoundException("Payment", paymentId);
        }

        return paymentMapper.toDto(payment);
    }

    @Override
    public PagedResponse<PaymentDto> getAllPayments(Pageable pageable) {
        Page<Payment> page = paymentRepository.findAll(pageable);

        return PagedResponse.<PaymentDto>builder()
                .content(page.getContent().stream().map(paymentMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
