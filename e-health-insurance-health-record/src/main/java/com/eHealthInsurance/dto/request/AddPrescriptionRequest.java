package com.eHealthInsurance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AddPrescriptionRequest(
    @NotBlank String medicationName,
    String dosage,
    String frequency,
    @NotNull LocalDate prescribedDate,
    LocalDate endDate,
    String status
) {}
