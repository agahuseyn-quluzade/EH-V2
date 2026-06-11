package com.eHealthInsurance.repository;

import com.eHealthInsurance.entity.HealthRecord;
import com.eHealthInsurance.entity.LabResult;
import com.eHealthInsurance.entity.MedicalEntry;
import com.eHealthInsurance.entity.Prescription;
import com.eHealthInsurance.entity.enums.EntryType;
import com.eHealthInsurance.entity.enums.RecordStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "ehi.security.jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RpbmctcHVycG9zZXMtb25seQ==",
        "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RpbmctcHVycG9zZXMtb25seQ==",
        "spring.datasource.url=jdbc:h2:mem:healthrecord-persistence",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "ehi.outbox.publisher.enabled=false"
})
class HealthRecordPersistenceIntegrationTest {
    @Autowired
    private HealthRecordRepository healthRecordRepository;
    @Autowired
    private MedicalEntryRepository medicalEntryRepository;
    @Autowired
    private PrescriptionRepository prescriptionRepository;
    @Autowired
    private LabResultRepository labResultRepository;

    @Test
    void persistsRecordChildrenAndQueriesByRecordId() {
        UUID memberId = UUID.randomUUID();
        HealthRecord record = healthRecordRepository.saveAndFlush(record(memberId));

        medicalEntryRepository.saveAndFlush(medicalEntry(record, LocalDate.now().minusDays(2)));
        medicalEntryRepository.saveAndFlush(medicalEntry(record, LocalDate.now()));
        prescriptionRepository.saveAndFlush(prescription(record, LocalDate.now()));
        labResultRepository.saveAndFlush(labResult(record, LocalDate.now()));

        assertThat(healthRecordRepository.findByMemberId(memberId)).contains(record);
        assertThat(medicalEntryRepository.findByHealthRecordIdOrderByEntryDateDesc(record.getId()))
                .hasSize(2)
                .extracting(MedicalEntry::getEntryDate)
                .isSortedAccordingTo((left, right) -> right.compareTo(left));
        assertThat(prescriptionRepository.findByHealthRecordIdOrderByPrescribedDateDesc(record.getId())).hasSize(1);
        assertThat(labResultRepository.findByHealthRecordIdOrderByTestDateDesc(record.getId())).hasSize(1);
    }

    @Test
    void memberIdIsUnique() {
        UUID memberId = UUID.randomUUID();
        healthRecordRepository.saveAndFlush(record(memberId));

        assertThatThrownBy(() -> healthRecordRepository.saveAndFlush(record(memberId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private HealthRecord record(UUID memberId) {
        HealthRecord record = new HealthRecord();
        record.setMemberId(memberId);
        record.setStatus(RecordStatus.ACTIVE);
        return record;
    }

    private MedicalEntry medicalEntry(HealthRecord record, LocalDate date) {
        MedicalEntry entry = new MedicalEntry();
        entry.setHealthRecord(record);
        entry.setEntryType(EntryType.VISIT);
        entry.setEntryDate(date);
        entry.setDescription("stored only in database, never audit payload");
        return entry;
    }

    private Prescription prescription(HealthRecord record, LocalDate date) {
        Prescription prescription = new Prescription();
        prescription.setHealthRecord(record);
        prescription.setMedicationName("Medication");
        prescription.setPrescribedDate(date);
        prescription.setStatus("ACTIVE");
        return prescription;
    }

    private LabResult labResult(HealthRecord record, LocalDate date) {
        LabResult result = new LabResult();
        result.setHealthRecord(record);
        result.setTestName("Blood Test");
        result.setTestDate(date);
        result.setStatus("COMPLETED");
        return result;
    }
}
