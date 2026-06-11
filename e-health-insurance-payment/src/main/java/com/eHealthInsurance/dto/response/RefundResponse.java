package com.eHealthInsurance.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
    UUID id,
    UUID paymentId,
    BigDecimal amount,
    String reason,
    String providerReference,
    String status,
    Instant requestedAt,
    Instant processedAt
) {}
