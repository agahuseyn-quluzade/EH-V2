package com.eHealthInsurance.service;

import com.eHealthInsurance.common.events.DomainEvent;
import com.eHealthInsurance.common.events.EventSchemaVersions;
import com.eHealthInsurance.common.events.audit.AuditEventCreatedEvent;
import com.eHealthInsurance.common.events.healthrecord.HealthRecordCreatedEvent;
import com.eHealthInsurance.common.events.healthrecord.HealthRecordUpdatedEvent;
import com.eHealthInsurance.dto.request.AddPrescriptionRequest;
import com.eHealthInsurance.dto.response.PrescriptionResponse;
import com.eHealthInsurance.entity.HealthRecord;
import com.eHealthInsurance.entity.Prescription;
import com.eHealthInsurance.entity.enums.RecordStatus;
import com.eHealthInsurance.mapper.HealthRecordMapper;
import com.eHealthInsurance.outbox.EventMetadata;
import com.eHealthInsurance.outbox.EventMetadataFactory;
import com.eHealthInsurance.outbox.OutboxService;
import com.eHealthInsurance.repository.HealthRecordRepository;
import com.eHealthInsurance.repository.LabResultRepository;
import com.eHealthInsurance.repository.MedicalEntryRepository;
import com.eHealthInsurance.repository.PrescriptionRepository;
import com.eHealthInsurance.service.impl.HealthRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceImplTest {
    @Mock
    private HealthRecordRepository healthRecordRepository;
    @Mock
    private MedicalEntryRepository medicalEntryRepository;
    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private LabResultRepository labResultRepository;
    @Mock
    private HealthRecordMapper mapper;
    @Mock
    private OutboxService outboxService;
    @Mock
    private EventMetadataFactory eventMetadataFactory;

    @Test
    void createHealthRecordPublishesDomainAndAuditEvents() {
        UUID memberId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        HealthRecord saved = record(recordId, memberId);

        when(healthRecordRepository.existsByMemberId(memberId)).thenReturn(false);
        when(healthRecordRepository.save(any(HealthRecord.class))).thenReturn(saved);
        when(eventMetadataFactory.create(eq(recordId), eq(memberId)))
                .thenReturn(metadata(recordId, memberId), metadata(recordId, memberId));
        when(medicalEntryRepository.findByHealthRecordIdOrderByEntryDateDesc(recordId)).thenReturn(List.of());
        when(prescriptionRepository.findByHealthRecordIdOrderByPrescribedDateDesc(recordId)).thenReturn(List.of());
        when(labResultRepository.findByHealthRecordIdOrderByTestDateDesc(recordId)).thenReturn(List.of());

        newService().createHealthRecord(memberId);

        List<DomainEvent> events = capturedEvents();
        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isInstanceOf(HealthRecordCreatedEvent.class);
        assertThat(events.get(0).topic()).isEqualTo("health-record.created");

        AuditEventCreatedEvent audit = (AuditEventCreatedEvent) events.get(1);
        assertThat(audit.topic()).isEqualTo("audit.event.created");
        assertThat(audit.action()).isEqualTo("HEALTH_RECORD_CREATED");
        assertThat(audit.resourceType()).isEqualTo("HEALTH_RECORD");
        assertThat(audit.resourceId()).isEqualTo(recordId);
        assertThat(audit.attributes()).containsEntry("containsMedicalPayload", false);
    }

    @Test
    void addPrescriptionPublishesUpdatedAndAuditEventsWithoutMedicalDetailsInAudit() {
        UUID memberId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        HealthRecord record = record(recordId, memberId);

        when(healthRecordRepository.findByMemberId(memberId)).thenReturn(Optional.of(record));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> {
            Prescription prescription = invocation.getArgument(0);
            prescription.setId(prescriptionId);
            return prescription;
        });
        when(eventMetadataFactory.create(eq(recordId), eq(memberId)))
                .thenReturn(metadata(recordId, memberId), metadata(recordId, memberId));
        when(mapper.toPrescriptionResponse(any(Prescription.class))).thenReturn(new PrescriptionResponse(
                prescriptionId,
                "Sensitive Medication",
                "10mg",
                "Daily",
                LocalDate.now(),
                null,
                "ACTIVE",
                Instant.now()
        ));

        newService().addPrescription(memberId, new AddPrescriptionRequest(
                "Sensitive Medication",
                "10mg",
                "Daily",
                LocalDate.now(),
                null,
                null
        ));

        List<DomainEvent> events = capturedEvents();
        assertThat(events).hasSize(2);

        HealthRecordUpdatedEvent updated = (HealthRecordUpdatedEvent) events.get(0);
        assertThat(updated.topic()).isEqualTo("health-record.updated");
        assertThat(updated.changedResourceType()).isEqualTo("PRESCRIPTION");
        assertThat(updated.changedResourceId()).isEqualTo(prescriptionId);

        AuditEventCreatedEvent audit = (AuditEventCreatedEvent) events.get(1);
        assertThat(audit.action()).isEqualTo("PRESCRIPTION_ADDED");
        assertThat(audit.attributes().values())
                .extracting(Object::toString)
                .doesNotContain("Sensitive Medication", "10mg", "Daily");
        assertThat(record.getUpdatedAt()).isNotNull();
    }

    private HealthRecordServiceImpl newService() {
        return new HealthRecordServiceImpl(
                healthRecordRepository,
                medicalEntryRepository,
                prescriptionRepository,
                labResultRepository,
                mapper,
                outboxService,
                eventMetadataFactory
        );
    }

    private List<DomainEvent> capturedEvents() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outboxService, times(2)).enqueue(captor.capture());
        return captor.getAllValues();
    }

    private HealthRecord record(UUID recordId, UUID memberId) {
        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setMemberId(memberId);
        record.setStatus(RecordStatus.ACTIVE);
        record.setCreatedAt(Instant.now());
        return record;
    }

    private EventMetadata metadata(UUID recordId, UUID memberId) {
        return new EventMetadata(
                EventSchemaVersions.V1,
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID().toString(),
                null,
                "e-health-insurance-health-record",
                recordId,
                memberId
        );
    }
}
