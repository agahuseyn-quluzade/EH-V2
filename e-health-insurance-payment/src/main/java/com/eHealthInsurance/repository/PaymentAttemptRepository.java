package com.eHealthInsurance.repository;

import com.eHealthInsurance.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    List<PaymentAttempt> findByPayment_IdOrderByCreatedAtDesc(UUID paymentId);
}
