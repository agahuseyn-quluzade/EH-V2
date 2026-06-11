package com.eHealthInsurance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AddLabResultRequest(
    @NotBlank String testName,
    @NotNull LocalDate testDate,
    String resultValue,
    String referenceRange,
    String unit,
    String status
) {}
