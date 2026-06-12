package com.ehi.payment.entity;

import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_transactions",
        indexes = {
                @Index(name = "idx_payment_transactions_order_id", columnList = "order_id", unique = true),
                @Index(name = "idx_payment_transactions_payment_id", columnList = "payment_id"),
                @Index(name = "idx_payment_transactions_reference", columnList = "reference_id,reference_type")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private PaymentReferenceType referenceType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String provider;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(length = 1024)
    private String redirectUrl;

    private String epointTransaction;

    private String bankTransaction;

    private String cardMask;

    private String cardName;

    private String gatewayStatus;

    private String gatewayCode;

    private String operationCode;

    @Column(length = 1024)
    private String failureReason;

    private Instant callbackReceivedAt;

    private Instant completedAt;

    private Instant reversedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
