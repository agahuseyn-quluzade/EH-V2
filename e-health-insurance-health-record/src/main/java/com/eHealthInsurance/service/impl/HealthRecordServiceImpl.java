package com.eHealthInsurance.service.impl;

import com.eHealthInsurance.common.events.audit.AuditEventCreatedEvent;
import com.eHealthInsurance.common.events.healthrecord.HealthRecordCreatedEvent;
import com.eHealthInsurance.common.events.healthrecord.HealthRecordUpdatedEvent;
import com.eHealthInsurance.dto.request.*;
import com.eHealthInsurance.dto.response.*;
import com.eHealthInsurance.entity.*;
import com.eHealthInsurance.entity.enums.RecordStatus;
import com.eHealthInsurance.exception.HealthRecordErrorEnum;
import com.eHealthInsurance.exception.HealthRecordException;
import com.eHealthInsurance.mapper.HealthRecordMapper;
import com.eHealthInsurance.outbox.EventMetadata;
import com.eHealthInsurance.outbox.EventMetadataFactory;
import com.eHealthInsurance.outbox.OutboxService;
import com.eHealthInsurance.repository.*;
import com.eHealthInsurance.service.HealthRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j @Service @RequiredArgsConstructor
@Transactional
public class HealthRecordServiceImpl implements HealthRecordService {

    private final HealthRecordRepository healthRecordRepository;
    private final MedicalEntryRepository medicalEntryRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LabResultRepository labResultRepository;
    private final HealthRecordMapper mapper;
    private final OutboxService outboxService;
    private final EventMetadataFactory eventMetadataFactory;

    @Override
    @Transactional(readOnly = true)
    public HealthSummaryResponse getHealthRecord(UUID memberId) {
        HealthRecord record = healthRecordRepository.findByMemberId(memberId)
                .orElseThrow(() -> new HealthRecordException(HealthRecordErrorEnum.RECORD_NOT_FOUND));
        return buildSummary(record);
    }

    @Override
    public HealthSummaryResponse createHealthRecord(UUID memberId) {
        if (healthRecordRepository.existsByMemberId(memberId)) {
            throw new HealthRecordException(HealthRecordErrorEnum.RECORD_ALREADY_EXISTS);
        }
        HealthRecord record = new HealthRecord();
        record.setMemberId(memberId);
        record.setStatus(RecordStatus.ACTIVE);
        record = healthRecordRepository.save(record);
        enqueueHealthRecordCreated(record);
        enqueueAuditEvent(record, "HEALTH_RECORD_CREATED", "HEALTH_RECORD", record.getId());
        log.info("Health record created: recordId={}", record.getId());
        return buildSummary(record);
    }

    @Override
    public HealthSummaryResponse updateHealthRecordStatus(UUID memberId, RecordStatus status) {
        HealthRecord record = getRecordForMember(memberId);
        record.setStatus(status);
        markRecordUpdated(record);
        record = healthRecordRepository.save(record);
        enqueueHealthRecordUpdated(record, "UPDATED", "HEALTH_RECORD_STATUS", record.getId());
        enqueueAuditEvent(record, "HEALTH_RECORD_STATUS_UPDATED", "HEALTH_RECORD", record.getId());
        log.info("Health record status updated: recordId={}, status={}", record.getId(), status);
        return buildSummary(record);
    }

    @Override
    public void deleteHealthRecord(UUID memberId) {
        HealthRecord record = getRecordForMember(memberId);
        record.setStatus(RecordStatus.DELETED);
        markRecordUpdated(record);
        record = healthRecordRepository.save(record);
        enqueueHealthRecordUpdated(record, "DELETED", "HEALTH_RECORD", record.getId());
        enqueueAuditEvent(record, "HEALTH_RECORD_DELETED", "HEALTH_RECORD", record.getId());
        log.info("Health record soft-deleted: recordId={}", record.getId());
    }

    @Override
    public MedicalEntryResponse addMedicalEntry(UUID memberId, AddMedicalEntryRequest request) {
        HealthRecord record = getRecordForMember(memberId);
        MedicalEntry entry = new MedicalEntry();
        entry.setHealthRecord(record);
        entry.setEntryType(request.entryType());
        entry.setDiagnosisCode(request.diagnosisCode());
        entry.setDescription(request.description());
        entry.setProviderName(request.providerName());
        entry.setEntryDate(request.entryDate());
        entry = medicalEntryRepository.save(entry);
        markRecordUpdated(record);
        enqueueHealthRecordUpdated(record, "ADDED", "MEDICAL_ENTRY", entry.getId());
        enqueueAuditEvent(record, "MEDICAL_ENTRY_ADDED", "MEDICAL_ENTRY", entry.getId());
        log.info("Medical entry added: recordId={}, entryId={}", record.getId(), entry.getId());
        return mapper.toMedicalEntryResponse(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalEntryResponse> getMedicalEntries(UUID memberId) {
        HealthRecord record = getRecordForMember(memberId);
        return mapper.toMedicalEntryResponseList(
                medicalEntryRepository.findByHealthRecordIdOrderByEntryDateDesc(record.getId()));
    }

    @Override
    public PrescriptionResponse addPrescription(UUID memberId, AddPrescriptionRequest request) {
        HealthRecord record = getRecordForMember(memberId);
        Prescription rx = new Prescription();
        rx.setHealthRecord(record);
        rx.setMedicationName(request.medicationName());
        rx.setDosage(request.dosage());
        rx.setFrequency(request.frequency());
        rx.setPrescribedDate(request.prescribedDate());
        rx.setEndDate(request.endDate());
        rx.setStatus(request.status() != null ? request.status() : "ACTIVE");
        rx = prescriptionRepository.save(rx);
        markRecordUpdated(record);
        enqueueHealthRecordUpdated(record, "ADDED", "PRESCRIPTION", rx.getId());
        enqueueAuditEvent(record, "PRESCRIPTION_ADDED", "PRESCRIPTION", rx.getId());
        log.info("Prescription added: recordId={}, prescriptionId={}", record.getId(), rx.getId());
        return mapper.toPrescriptionResponse(rx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptions(UUID memberId) {
        HealthRecord record = getRecordForMember(memberId);
        return mapper.toPrescriptionResponseList(
                prescriptionRepository.findByHealthRecordIdOrderByPrescribedDateDesc(record.getId()));
    }

    @Override
    public LabResultResponse addLabResult(UUID memberId, AddLabResultRequest request) {
        HealthRecord record = getRecordForMember(memberId);
        LabResult lab = new LabResult();
        lab.setHealthRecord(record);
        lab.setTestName(request.testName());
        lab.setTestDate(request.testDate());
        lab.setResultValue(request.resultValue());
        lab.setReferenceRange(request.referenceRange());
        lab.setUnit(request.unit());
        lab.setStatus(request.status() != null ? request.status() : "COMPLETED");
        lab = labResultRepository.save(lab);
        markRecordUpdated(record);
        enqueueHealthRecordUpdated(record, "ADDED", "LAB_RESULT", lab.getId());
        enqueueAuditEvent(record, "LAB_RESULT_ADDED", "LAB_RESULT", lab.getId());
        log.info("Lab result added: recordId={}, labResultId={}", record.getId(), lab.getId());
        return mapper.toLabResultResponse(lab);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabResultResponse> getLabResults(UUID memberId) {
        HealthRecord record = getRecordForMember(memberId);
        return mapper.toLabResultResponseList(
                labResultRepository.findByHealthRecordIdOrderByTestDateDesc(record.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public HealthSummaryResponse getInternalSummary(UUID memberId) {
        HealthRecord record = healthRecordRepository.findByMemberId(memberId).orElse(null);
        if (record == null) {
            return new HealthSummaryResponse(null, memberId, "NO_RECORD", null, 0, 0, 0, List.of(), List.of(), List.of());
        }
        return buildSummary(record);
    }

    private HealthRecord getRecordForMember(UUID memberId) {
        return healthRecordRepository.findByMemberId(memberId)
                .orElseThrow(() -> new HealthRecordException(HealthRecordErrorEnum.RECORD_NOT_FOUND));
    }

    private HealthSummaryResponse buildSummary(HealthRecord record) {
        List<MedicalEntry> entries = medicalEntryRepository.findByHealthRecordIdOrderByEntryDateDesc(record.getId());
        List<Prescription> prescriptions = prescriptionRepository.findByHealthRecordIdOrderByPrescribedDateDesc(record.getId());
        List<LabResult> labResults = labResultRepository.findByHealthRecordIdOrderByTestDateDesc(record.getId());

        return new HealthSummaryResponse(
            record.getId(), record.getMemberId(), record.getStatus().name(),
            record.getCreatedAt(), entries.size(), prescriptions.size(), labResults.size(),
            mapper.toMedicalEntryResponseList(entries),
            mapper.toPrescriptionResponseList(prescriptions),
            mapper.toLabResultResponseList(labResults)
        );
    }

    private void enqueueHealthRecordCreated(HealthRecord record) {
        EventMetadata metadata = eventMetadataFactory.create(record.getId(), record.getMemberId());
        outboxService.enqueue(new HealthRecordCreatedEvent(
                metadata.schemaVersion(),
                metadata.eventId(),
                metadata.occurredAt(),
                metadata.correlationId(),
                metadata.causationId(),
                metadata.producer(),
                metadata.aggregateId(),
                metadata.userId(),
                record.getId(),
                record.getMemberId(),
                record.getStatus().name(),
                record.getCreatedAt()
        ));
    }

    private void enqueueHealthRecordUpdated(HealthRecord record, String changeType, String resourceType, UUID resourceId) {
        EventMetadata metadata = eventMetadataFactory.create(record.getId(), record.getMemberId());
        outboxService.enqueue(new HealthRecordUpdatedEvent(
                metadata.schemaVersion(),
                metadata.eventId(),
                metadata.occurredAt(),
                metadata.correlationId(),
                metadata.causationId(),
                metadata.producer(),
                metadata.aggregateId(),
                metadata.userId(),
                record.getId(),
                record.getMemberId(),
                record.getStatus().name(),
                changeType,
                resourceType,
                resourceId,
                Instant.now()
        ));
    }

    private void enqueueAuditEvent(HealthRecord record, String action, String resourceType, UUID resourceId) {
        EventMetadata metadata = eventMetadataFactory.create(record.getId(), record.getMemberId());
        outboxService.enqueue(new AuditEventCreatedEvent(
                metadata.schemaVersion(),
                metadata.eventId(),
                metadata.occurredAt(),
                metadata.correlationId(),
                metadata.causationId(),
                metadata.producer(),
                metadata.aggregateId(),
                metadata.userId(),
                action,
                resourceType,
                resourceId,
                "SUCCESS",
                Map.of(
                        "service", "health-record",
                        "containsMedicalPayload", false
                )
        ));
    }

    private void markRecordUpdated(HealthRecord record) {
        record.setUpdatedAt(Instant.now());
    }
}
