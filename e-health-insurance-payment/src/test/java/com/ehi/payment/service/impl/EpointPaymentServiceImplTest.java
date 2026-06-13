package com.ehi.payment.service.impl;

import com.ehi.payment.client.EpointCheckoutResponse;
import com.ehi.payment.client.EpointClient;
import com.ehi.payment.client.EpointPaymentResult;
import com.ehi.payment.client.EpointSignature;
import com.ehi.payment.config.EpointProperties;
import com.ehi.payment.dto.response.CardRegistrationResponse;
import com.ehi.payment.entity.Payment;
import com.ehi.payment.entity.SavedCard;
import com.ehi.payment.kafka.PaymentCompletedEventProducer;
import com.ehi.payment.kafka.PaymentFailedEventProducer;
import com.ehi.payment.mapper.PaymentMapper;
import com.ehi.payment.mapper.SavedCardMapper;
import com.ehi.payment.repository.PaymentRepository;
import com.ehi.payment.repository.SavedCardRepository;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import com.ehi.infra.event.PaymentCompletedEvent;
import com.ehi.infra.event.PaymentFailedEvent;
import com.ehi.infra.exception.NotFoundException;
import com.ehi.infra.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpointPaymentServiceImplTest {

    private static final String PRIVATE_KEY = "test-private-key";

    @Mock PaymentRepository paymentRepository;
    @Mock SavedCardRepository savedCardRepository;
    @Mock EpointClient epointClient;
    @Mock PaymentMapper paymentMapper;
    @Mock SavedCardMapper savedCardMapper;
    @Mock PaymentCompletedEventProducer paymentCompletedEventProducer;
    @Mock PaymentFailedEventProducer paymentFailedEventProducer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EpointPaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        EpointProperties properties = new EpointProperties();
        properties.setPrivateKey(PRIVATE_KEY);
        service = new EpointPaymentServiceImpl(paymentRepository, savedCardRepository, properties,
                epointClient, paymentMapper, savedCardMapper, objectMapper,
                paymentCompletedEventProducer, paymentFailedEventProducer);
    }

    private Payment pendingPayment() {
        return Payment.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .referenceId(UUID.randomUUID())
                .referenceType(PaymentReferenceType.POLICY_PREMIUM)
                .amount(BigDecimal.valueOf(150))
                .status(PaymentStatus.PENDING)
                .build();
    }

    private String encode(Map<String, Object> payload) {
        try {
            return Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(payload));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void callback(Map<String, Object> payload) {
        String data = encode(payload);
        service.handleCallback(data, EpointSignature.sign(PRIVATE_KEY, data));
    }

    // ---- handleCallback ----

    @Test
    void handleCallback_rejectsInvalidSignature() {
        String data = encode(Map.of("order_id", UUID.randomUUID().toString(), "status", "success"));

        assertThatThrownBy(() -> service.handleCallback(data, "bogus-signature"))
                .isInstanceOf(ServiceException.class);

        verify(paymentRepository, never()).findById(any());
    }

    @Test
    void handleCallback_success_completesPayment_andPublishesEvent() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("order_id", payment.getId().toString());
        payload.put("status", "success");
        payload.put("transaction", "tw000123");
        payload.put("bank_transaction", "bank-1");
        payload.put("rrn", "rrn-1");
        payload.put("card_mask", "123456******1234");
        payload.put("amount", "150");
        callback(payload);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getTransactionId()).isEqualTo("tw000123");
        assertThat(payment.getEpointTransaction()).isEqualTo("tw000123");
        assertThat(payment.getBankTransaction()).isEqualTo("bank-1");
        assertThat(payment.getRrn()).isEqualTo("rrn-1");
        assertThat(payment.getCardMask()).isEqualTo("123456******1234");
        verify(paymentRepository).save(payment);

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentCompletedEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().paymentId()).isEqualTo(payment.getId());
    }

    @Test
    void handleCallback_duplicateSuccess_isIdempotent() {
        Payment payment = pendingPayment();
        payment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        callback(Map.of("order_id", payment.getId().toString(), "status", "success", "transaction", "tw000123"));

        verify(paymentRepository, never()).save(any());
        verify(paymentCompletedEventProducer, never()).publish(any(PaymentCompletedEvent.class));
    }

    @Test
    void handleCallback_failed_failsPayment_andPublishesEvent() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        callback(Map.of("order_id", payment.getId().toString(), "status", "failed",
                "code", "116", "message", "Declined, insufficient funds"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Declined, insufficient funds");

        ArgumentCaptor<PaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(paymentFailedEventProducer).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().reason()).isEqualTo("Declined, insufficient funds");
    }

    @Test
    void handleCallback_unknownPayment_isIgnored() {
        UUID unknownId = UUID.randomUUID();
        when(paymentRepository.findById(unknownId)).thenReturn(Optional.empty());

        callback(Map.of("order_id", unknownId.toString(), "status", "success"));

        verify(paymentRepository, never()).save(any());
        verify(paymentCompletedEventProducer, never()).publish(any(PaymentCompletedEvent.class));
    }

    @Test
    void handleCallback_cardRegistrationSuccess_activatesCard() {
        SavedCard card = SavedCard.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).cardId("ce123").active(false).build();
        when(savedCardRepository.findByCardId("ce123")).thenReturn(Optional.of(card));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("card_id", "ce123");
        payload.put("status", "success");
        payload.put("operation_code", "001");
        payload.put("card_mask", "123456******1234");
        payload.put("card_name", "JOHN DOE");
        callback(payload);

        assertThat(card.isActive()).isTrue();
        assertThat(card.getCardMask()).isEqualTo("123456******1234");
        assertThat(card.getCardName()).isEqualTo("JOHN DOE");
        verify(savedCardRepository).save(card);
    }

    @Test
    void handleCallback_cardRegistrationFailed_leavesCardInactive() {
        SavedCard card = SavedCard.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).cardId("ce123").active(false).build();
        when(savedCardRepository.findByCardId("ce123")).thenReturn(Optional.of(card));

        callback(Map.of("card_id", "ce123", "status", "failed", "message", "Rejected"));

        assertThat(card.isActive()).isFalse();
        verify(savedCardRepository, never()).save(any());
    }

    // ---- refreshStatus ----

    @Test
    void refreshStatus_throwsNotFound_forNonOwner() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.refreshStatus(payment.getId(), UUID.randomUUID(), false))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void refreshStatus_throws_whenNoEpointTransaction() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.refreshStatus(payment.getId(), payment.getUserId(), false))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void refreshStatus_appliesSuccessResult() {
        Payment payment = pendingPayment();
        payment.setEpointTransaction("tw000123");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(epointClient.getStatus("tw000123")).thenReturn(EpointPaymentResult.builder()
                .status("success").transaction("tw000123").rrn("rrn-1").build());

        service.refreshStatus(payment.getId(), payment.getUserId(), false);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentCompletedEventProducer).publish(any(PaymentCompletedEvent.class));
        verify(paymentMapper).toDto(payment);
    }

    // ---- card registration ----

    @Test
    void startCardRegistration_savesInactiveCard_andReturnsRedirect() {
        UUID userId = UUID.randomUUID();
        when(epointClient.registerPayoutCard(anyString())).thenReturn(EpointCheckoutResponse.builder()
                .status("success").cardId("ce123").redirectUrl("https://epoint.az/register/ce123").build());
        when(savedCardRepository.save(any(SavedCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardRegistrationResponse response = service.startCardRegistration(userId);

        assertThat(response.redirectUrl()).isEqualTo("https://epoint.az/register/ce123");

        ArgumentCaptor<SavedCard> captor = ArgumentCaptor.forClass(SavedCard.class);
        verify(savedCardRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getCardId()).isEqualTo("ce123");
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void startCardRegistration_throws_whenEpointRejects() {
        when(epointClient.registerPayoutCard(anyString())).thenReturn(EpointCheckoutResponse.builder()
                .status("error").message("Invalid merchant").build());

        assertThatThrownBy(() -> service.startCardRegistration(UUID.randomUUID()))
                .isInstanceOf(ServiceException.class);

        verify(savedCardRepository, never()).save(any());
    }
}
