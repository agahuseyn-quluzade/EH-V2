package com.eHealthInsurance.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    UUID policyId,
    BigDecimal amount,
    LocalDate dueDate,
    String status,
    String invoiceNumber,
    Instant createdAt
) {}
