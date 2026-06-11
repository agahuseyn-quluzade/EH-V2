package com.eHealthInsurance.dto.request;

import com.eHealthInsurance.entity.enums.EntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AddMedicalEntryRequest(
    @NotNull EntryType entryType,
    String diagnosisCode,
    String description,
    String providerName,
    @NotNull LocalDate entryDate
) {}
