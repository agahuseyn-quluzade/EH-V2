package com.eHealthInsurance.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PolicyResponse(
    UUID id,
    String planName,
    BigDecimal planMonthlyPremium,
    String status
) {}
