package com.eHealthInsurance.repository;

import com.eHealthInsurance.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByMemberIdOrderByCreatedAtDesc(UUID memberId);

    Optional<Payment> findByIdAndMemberId(UUID id, UUID memberId);

    Optional<Payment> findByMemberIdAndIdempotencyKey(UUID memberId, String idempotencyKey);
}
