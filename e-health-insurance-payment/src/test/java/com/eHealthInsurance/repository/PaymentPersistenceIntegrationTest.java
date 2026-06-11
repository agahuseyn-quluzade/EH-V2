package com.eHealthInsurance.repository;

import com.eHealthInsurance.entity.Payment;
import com.eHealthInsurance.entity.PaymentAttempt;
import com.eHealthInsurance.entity.enums.PaymentAttemptStatus;
import com.eHealthInsurance.entity.enums.PaymentProvider;
import com.eHealthInsurance.entity.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class PaymentPersistenceIntegrationTest {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentAttemptRepository paymentAttemptRepository;

    @Test
    void enforcesMemberScopedIdempotencyKeyUniqueness() {
        UUID memberId = UUID.randomUUID();

        paymentRepository.saveAndFlush(payment(memberId, "idem-duplicate"));

        Payment duplicate = payment(memberId, "idem-duplicate");
        assertThrows(DataIntegrityViolationException.class, () -> paymentRepository.saveAndFlush(duplicate));
    }

    @Test
    void persistsPaymentAttemptForPayment() {
        Payment payment = paymentRepository.saveAndFlush(payment(UUID.randomUUID(), "idem-attempt"));
        paymentAttemptRepository.saveAndFlush(PaymentAttempt.builder()
                .payment(payment)
                .provider(PaymentProvider.MOCK)
                .status(PaymentAttemptStatus.INITIATED)
                .providerReference("mock_ref")
                .idempotencyKey("idem-attempt")
                .build());

        assertEquals(1, paymentAttemptRepository.findByPayment_IdOrderByCreatedAtDesc(payment.getId()).size());
    }

    private Payment payment(UUID memberId, String idempotencyKey) {
        return Payment.builder()
                .policyId(UUID.randomUUID())
                .memberId(memberId)
                .amount(BigDecimal.valueOf(100))
                .currency("AZN")
                .provider(PaymentProvider.MOCK)
                .status(PaymentStatus.PROCESSING)
                .providerReference("mock_ref")
                .idempotencyKey(idempotencyKey)
                .build();
    }
}
