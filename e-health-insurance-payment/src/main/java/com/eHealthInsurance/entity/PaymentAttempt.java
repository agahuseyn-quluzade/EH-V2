package com.eHealthInsurance.entity;

import com.eHealthInsurance.entity.enums.PaymentAttemptStatus;
import com.eHealthInsurance.entity.enums.PaymentProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_attempt",
        indexes = {
                @Index(name = "idx_payment_attempt_payment", columnList = "payment_id"),
                @Index(name = "idx_payment_attempt_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAttempt {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentAttemptStatus status;

    @Column(name = "provider_reference", length = 200)
    private String providerReference;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
