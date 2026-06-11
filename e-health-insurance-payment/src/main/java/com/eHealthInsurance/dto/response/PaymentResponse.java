package com.eHealthInsurance.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID policyId,
    UUID memberId,
    BigDecimal amount,
    String currency,
    String provider,
    String status,
    String providerReference,
    String failureCode,
    String failureReason,
    Instant paidAt,
    Instant failedAt,
    Instant createdAt
) {}
