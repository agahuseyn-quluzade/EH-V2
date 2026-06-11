package com.eHealthInsurance.dto.response;

import com.eHealthInsurance.entity.enums.EntryType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MedicalEntryResponse(
    UUID id, EntryType entryType, String diagnosisCode,
    String description, String providerName,
    LocalDate entryDate, Instant createdAt
) {}
