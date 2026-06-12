package com.ehi.payment.service.impl;

import com.ehi.payment.dto.response.PaymentDto;
import com.ehi.payment.entity.Payment;
import com.ehi.payment.kafka.PaymentFailedEventProducer;
import com.ehi.payment.mapper.PaymentMapper;
import com.ehi.payment.repository.PaymentRepository;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import com.ehi.infra.event.PaymentFailedEvent;
import com.ehi.infra.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentMapper paymentMapper;
    @Mock PaymentFailedEventProducer paymentFailedEventProducer;
    @Mock MockPaymentProcessor mockPaymentProcessor;

    @InjectMocks PaymentServiceImpl paymentService;

    private PaymentDto toDtoStub(Payment payment) {
        return PaymentDto.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .referenceId(payment.getReferenceId())
                .referenceType(payment.getReferenceType())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .failureReason(payment.getFailureReason())
                .build();
    }

    // ---- processPayment ----

    @Test
    void processPayment_savesPending_andHandsOffToProcessor_forPositiveAmount() {
        when(paymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toDto(any(Payment.class))).thenAnswer(invocation -> toDtoStub(invocation.getArgument(0)));

        UUID userId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        PaymentDto result = paymentService.processPayment(userId, referenceId, PaymentReferenceType.POLICY_PREMIUM, BigDecimal.valueOf(100));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getReferenceId()).isEqualTo(referenceId);

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);

        verify(mockPaymentProcessor).process(saved.getId());
        verify(paymentFailedEventProducer, never()).publish(any(PaymentFailedEvent.class));
    }

    @Test
    void processPayment_savesFailed_andPublishesEvent_forZeroAmount() {
        when(paymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toDto(any(Payment.class))).thenAnswer(invocation -> toDtoStub(invocation.getArgument(0)));

        PaymentDto result = paymentService.processPayment(UUID.randomUUID(), UUID.randomUUID(), PaymentReferenceType.CLAIM_PAYOUT, BigDecimal.ZERO);

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("Invalid payment amount");

        verify(mockPaymentProcessor, never()).process(any());

        ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(paymentFailedEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().reason()).isEqualTo("Invalid payment amount");
    }

    @Test
    void processPayment_savesFailed_andPublishesEvent_forNegativeAmount() {
        when(paymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toDto(any(Payment.class))).thenAnswer(invocation -> toDtoStub(invocation.getArgument(0)));

        PaymentDto result = paymentService.processPayment(UUID.randomUUID(), UUID.randomUUID(), PaymentReferenceType.POLICY_PREMIUM, BigDecimal.valueOf(-50));

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentFailedEventProducer).publish(any(PaymentFailedEvent.class));
        verify(mockPaymentProcessor, never()).process(any());
    }

    @Test
    void processPayment_idempotent_returnsExistingPayment_whenNonFailedPaymentExists() {
        UUID referenceId = UUID.randomUUID();
        Payment existing = Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .referenceId(referenceId)
                .referenceType(PaymentReferenceType.POLICY_PREMIUM)
                .amount(BigDecimal.valueOf(100))
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(referenceId, PaymentReferenceType.POLICY_PREMIUM, PaymentStatus.FAILED))
                .thenReturn(Optional.of(existing));
        when(paymentMapper.toDto(existing)).thenReturn(toDtoStub(existing));

        PaymentDto result = paymentService.processPayment(existing.getUserId(), referenceId, PaymentReferenceType.POLICY_PREMIUM, BigDecimal.valueOf(100));

        assertThat(result.id()).isEqualTo(existing.getId());

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(mockPaymentProcessor, never()).process(any());
        verify(paymentFailedEventProducer, never()).publish(any(PaymentFailedEvent.class));
    }

    @Test
    void processPayment_createsNew_whenExistingPaymentForReferenceIsFailed() {
        UUID referenceId = UUID.randomUUID();

        when(paymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(referenceId, PaymentReferenceType.CLAIM_PAYOUT, PaymentStatus.FAILED))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toDto(any(Payment.class))).thenAnswer(invocation -> toDtoStub(invocation.getArgument(0)));

        PaymentDto result = paymentService.processPayment(UUID.randomUUID(), referenceId, PaymentReferenceType.CLAIM_PAYOUT, BigDecimal.valueOf(200));

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository).save(any(Payment.class));
        verify(mockPaymentProcessor).process(any());
    }

    // ---- getPaymentById ----

    @Test
    void getPaymentById_throwsNotFound_whenMissing() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId, UUID.randomUUID(), false))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPaymentById_throwsNotFound_whenNonOwnerNonPrivileged() {
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Payment payment = Payment.builder().id(UUID.randomUUID()).userId(ownerId)
                .referenceId(UUID.randomUUID()).referenceType(PaymentReferenceType.POLICY_PREMIUM)
                .amount(BigDecimal.TEN).status(PaymentStatus.COMPLETED).build();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPaymentById(payment.getId(), requesterId, false))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getPaymentById_allowsPrivilegedRequester_regardlessOfOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Payment payment = Payment.builder().id(UUID.randomUUID()).userId(ownerId)
                .referenceId(UUID.randomUUID()).referenceType(PaymentReferenceType.POLICY_PREMIUM)
                .amount(BigDecimal.TEN).status(PaymentStatus.COMPLETED).build();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentMapper.toDto(payment)).thenReturn(toDtoStub(payment));

        PaymentDto result = paymentService.getPaymentById(payment.getId(), adminId, true);

        assertThat(result.userId()).isEqualTo(ownerId);
    }

    // ---- getAllPayments ----

    @Test
    void getAllPayments_returnsPagedResponse() {
        Payment payment = Payment.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                .referenceId(UUID.randomUUID()).referenceType(PaymentReferenceType.POLICY_PREMIUM)
                .amount(BigDecimal.TEN).status(PaymentStatus.COMPLETED).build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Payment> page = new PageImpl<>(List.of(payment), pageable, 1);
        when(paymentRepository.findAll(pageable)).thenReturn(page);
        when(paymentMapper.toDto(payment)).thenReturn(toDtoStub(payment));

        var result = paymentService.getAllPayments(pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.last()).isTrue();
    }
}
