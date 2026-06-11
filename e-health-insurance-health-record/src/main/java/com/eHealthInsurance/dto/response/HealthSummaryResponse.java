package com.eHealthInsurance.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HealthSummaryResponse(
    UUID recordId, UUID memberId, String status, Instant createdAt,
    int totalEntries, int totalPrescriptions, int totalLabResults,
    List<MedicalEntryResponse> recentEntries,
    List<PrescriptionResponse> activePrescriptions,
    List<LabResultResponse> recentLabResults
) {}
