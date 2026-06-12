package com.ehi.payment.repository;

import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import com.ehi.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByOrderId(String orderId);

    Optional<PaymentTransaction> findByEpointTransaction(String epointTransaction);

    Optional<PaymentTransaction> findFirstByReferenceIdAndReferenceTypeAndStatusNotOrderByCreatedAtDesc(
            UUID referenceId, PaymentReferenceType referenceType, PaymentStatus status);
}
