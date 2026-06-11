package com.eHealthInsurance.mapper;

import com.eHealthInsurance.dto.response.*;
import com.eHealthInsurance.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface HealthRecordMapper {
    MedicalEntryResponse toMedicalEntryResponse(MedicalEntry entity);
    List<MedicalEntryResponse> toMedicalEntryResponseList(List<MedicalEntry> entities);

    PrescriptionResponse toPrescriptionResponse(Prescription entity);
    List<PrescriptionResponse> toPrescriptionResponseList(List<Prescription> entities);

    LabResultResponse toLabResultResponse(LabResult entity);
    List<LabResultResponse> toLabResultResponseList(List<LabResult> entities);
}
