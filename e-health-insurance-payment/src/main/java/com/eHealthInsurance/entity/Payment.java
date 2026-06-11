package com.eHealthInsurance.entity;

import com.eHealthInsurance.entity.enums.PaymentProvider;
import com.eHealthInsurance.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_member_idempotency", columnNames = {"member_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_payment_member_status", columnList = "member_id,status"),
                @Index(name = "idx_payment_policy", columnList = "policy_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "policy_id", nullable = false, columnDefinition = "uuid")
    private UUID policyId;

    @Column(name = "member_id", nullable = false, columnDefinition = "uuid")
    private UUID memberId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "provider_reference", length = 200)
    private String providerReference;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        if (currency == null || currency.isBlank()) {
            currency = "AZN";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
