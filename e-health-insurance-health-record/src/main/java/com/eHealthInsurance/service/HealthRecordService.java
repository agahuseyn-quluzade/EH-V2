package com.eHealthInsurance.service;

import com.eHealthInsurance.dto.request.*;
import com.eHealthInsurance.dto.response.*;
import com.eHealthInsurance.entity.enums.RecordStatus;
import java.util.List;
import java.util.UUID;

public interface HealthRecordService {
    HealthSummaryResponse getHealthRecord(UUID memberId);
    HealthSummaryResponse createHealthRecord(UUID memberId);
    HealthSummaryResponse updateHealthRecordStatus(UUID memberId, RecordStatus status);
    void deleteHealthRecord(UUID memberId);
    MedicalEntryResponse addMedicalEntry(UUID memberId, AddMedicalEntryRequest request);
    List<MedicalEntryResponse> getMedicalEntries(UUID memberId);
    PrescriptionResponse addPrescription(UUID memberId, AddPrescriptionRequest request);
    List<PrescriptionResponse> getPrescriptions(UUID memberId);
    LabResultResponse addLabResult(UUID memberId, AddLabResultRequest request);
    List<LabResultResponse> getLabResults(UUID memberId);
    HealthSummaryResponse getInternalSummary(UUID memberId);
}
