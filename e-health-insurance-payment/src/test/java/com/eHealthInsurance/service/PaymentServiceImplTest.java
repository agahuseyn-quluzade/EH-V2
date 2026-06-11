package com.eHealthInsurance.service;

import com.eHealthInsurance.client.IamServiceClient;
import com.eHealthInsurance.client.PolicyServiceClient;
import com.eHealthInsurance.client.dto.MemberStatusResponse;
import com.eHealthInsurance.client.dto.PolicyResponse;
import com.eHealthInsurance.common.events.payment.PaymentFailedEvent;
import com.eHealthInsurance.common.events.payment.PaymentInitiatedEvent;
import com.eHealthInsurance.common.events.payment.PaymentSucceededEvent;
import com.eHealthInsurance.dto.request.ConfirmPaymentRequest;
import com.eHealthInsurance.dto.request.CreatePaymentRequest;
import com.eHealthInsurance.dto.request.FailPaymentRequest;
import com.eHealthInsurance.dto.response.PaymentResponse;
import com.eHealthInsurance.entity.Payment;
import com.eHealthInsurance.entity.PaymentAttempt;
import com.eHealthInsurance.entity.enums.PaymentAttemptStatus;
import com.eHealthInsurance.entity.enums.PaymentProvider;
import com.eHealthInsurance.entity.enums.PaymentStatus;
import com.eHealthInsurance.mapper.PaymentMapper;
import com.eHealthInsurance.outbox.EventMetadata;
import com.eHealthInsurance.outbox.EventMetadataFactory;
import com.eHealthInsurance.outbox.OutboxService;
import com.eHealthInsurance.repository.InvoiceRepository;
import com.eHealthInsurance.repository.PaymentAttemptRepository;
import com.eHealthInsurance.repository.PaymentRepository;
import com.eHealthInsurance.repository.RefundRepository;
import com.eHealthInsurance.service.idempotency.PaymentIdempotencyService;
import com.eHealthInsurance.service.impl.PaymentServiceImpl;
import com.eHealthInsurance.service.provider.PaymentProviderClient;
import com.eHealthInsurance.service.provider.PaymentProviderRegistry;
import com.eHealthInsurance.service.provider.ProviderFailureResult;
import com.eHealthInsurance.service.provider.ProviderPaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private PaymentAttemptRepository paymentAttemptRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private PolicyServiceClient policyServiceClient;
    @Mock private IamServiceClient iamServiceClient;
    @Mock private OutboxService outboxService;
    @Mock private EventMetadataFactory eventMetadataFactory;
    @Mock private PaymentIdempotencyService idempotencyService;
    @Mock private PaymentProviderRegistry providerRegistry;
    @Mock private PaymentProviderClient providerClient;

    private PaymentServiceImpl service;
    private UUID memberId;
    private UUID policyId;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(
                paymentRepository,
                invoiceRepository,
                refundRepository,
                paymentAttemptRepository,
                paymentMapper,
                policyServiceClient,
                iamServiceClient,
                outboxService,
                eventMetadataFactory,
                idempotencyService,
                providerRegistry
        );
        memberId = UUID.randomUUID();
        policyId = UUID.randomUUID();

        when(eventMetadataFactory.create(any(UUID.class), any(UUID.class))).thenAnswer(invocation ->
                new EventMetadata(
                        "1.0",
                        UUID.randomUUID(),
                        Instant.now(),
                        "correlation-id",
                        null,
                        "payment-test",
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                ));
        when(paymentMapper.toResponse(any(Payment.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));
    }

    @Test
    void createPaymentInitiatesProviderStoresAttemptAndOutboxEvent() {
        when(idempotencyService.findPaymentId(memberId, "idem-1")).thenReturn(Optional.empty());
        when(idempotencyService.acquireLock(memberId, "idem-1")).thenReturn(true);
        when(policyServiceClient.getPolicy(policyId)).thenReturn(new PolicyResponse(policyId, "Gold", BigDecimal.TEN, "ACTIVE"));
        when(iamServiceClient.getMemberStatus(memberId)).thenReturn(new MemberStatusResponse(memberId, "member@test.local", true));
        when(providerRegistry.get(PaymentProvider.MOCK)).thenReturn(providerClient);
        when(providerClient.initiate(any(Payment.class), eq("tokenized-method"))).thenReturn(new ProviderPaymentResult("mock_init_ref"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.createPayment(
                memberId,
                new CreatePaymentRequest(policyId, BigDecimal.valueOf(100), PaymentProvider.MOCK, null, "tokenized-method"),
                "idem-1"
        );

        assertEquals("PROCESSING", response.status());
        assertEquals("mock_init_ref", response.providerReference());

        ArgumentCaptor<PaymentAttempt> attemptCaptor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentAttemptRepository).save(attemptCaptor.capture());
        assertEquals(PaymentAttemptStatus.INITIATED, attemptCaptor.getValue().getStatus());
        assertEquals("idem-1", attemptCaptor.getValue().getIdempotencyKey());

        verify(outboxService).enqueue(any(PaymentInitiatedEvent.class));
        verify(idempotencyService).rememberPayment(eq(memberId), eq("idem-1"), any(UUID.class));
        verify(idempotencyService).releaseLock(memberId, "idem-1");
    }

    @Test
    void confirmPaymentStoresSuccessAttemptAndSucceededEvent() {
        Payment payment = payment(PaymentStatus.PROCESSING);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(providerRegistry.get(PaymentProvider.MOCK)).thenReturn(providerClient);
        when(providerClient.confirm(payment, "provider-success")).thenReturn(new ProviderPaymentResult("provider-success"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.confirmPayment(payment.getId(), new ConfirmPaymentRequest("provider-success"));

        assertEquals("COMPLETED", response.status());
        assertNotNull(response.paidAt());
        verify(paymentAttemptRepository).save(any(PaymentAttempt.class));
        verify(outboxService).enqueue(any(PaymentSucceededEvent.class));
    }

    @Test
    void failPaymentStoresFailureAttemptAndFailedEvent() {
        Payment payment = payment(PaymentStatus.PROCESSING);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(providerRegistry.get(PaymentProvider.MOCK)).thenReturn(providerClient);
        when(providerClient.fail(payment, "provider-fail", "DECLINED", "Card declined"))
                .thenReturn(new ProviderFailureResult("provider-fail", "DECLINED", "Card declined"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.failPayment(
                payment.getId(),
                new FailPaymentRequest("provider-fail", "DECLINED", "Card declined")
        );

        assertEquals("FAILED", response.status());
        assertEquals("DECLINED", response.failureCode());
        assertNotNull(response.failedAt());
        verify(paymentAttemptRepository).save(any(PaymentAttempt.class));
        verify(outboxService).enqueue(any(PaymentFailedEvent.class));
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = Payment.builder()
                .policyId(policyId)
                .memberId(memberId)
                .amount(BigDecimal.valueOf(100))
                .currency("AZN")
                .provider(PaymentProvider.MOCK)
                .status(status)
                .idempotencyKey("idem-1")
                .providerReference("mock_init_ref")
                .build();
        payment.prePersist();
        return payment;
    }

    private PaymentResponse response(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPolicyId(),
                payment.getMemberId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProvider().name(),
                payment.getStatus().name(),
                payment.getProviderReference(),
                payment.getFailureCode(),
                payment.getFailureReason(),
                payment.getPaidAt(),
                payment.getFailedAt(),
                payment.getCreatedAt()
        );
    }
}
