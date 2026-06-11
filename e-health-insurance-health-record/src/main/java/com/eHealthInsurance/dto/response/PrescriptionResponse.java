package com.eHealthInsurance.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PrescriptionResponse(
    UUID id, String medicationName, String dosage, String frequency,
    LocalDate prescribedDate, LocalDate endDate, String status, Instant createdAt
) {}
