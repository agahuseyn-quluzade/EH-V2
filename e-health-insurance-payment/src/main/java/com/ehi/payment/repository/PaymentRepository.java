package com.ehi.payment.repository;

import com.ehi.payment.entity.Payment;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByUserId(UUID userId);

    Optional<Payment> findFirstByReferenceIdAndReferenceTypeAndStatusNot(
            UUID referenceId, PaymentReferenceType referenceType, PaymentStatus status);
}
