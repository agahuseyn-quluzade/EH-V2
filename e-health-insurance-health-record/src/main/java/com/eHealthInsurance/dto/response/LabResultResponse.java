package com.eHealthInsurance.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LabResultResponse(
    UUID id, String testName, LocalDate testDate,
    String resultValue, String referenceRange, String unit,
    String status, Instant createdAt
) {}
