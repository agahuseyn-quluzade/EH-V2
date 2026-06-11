package com.eHealthInsurance.entity;

import com.eHealthInsurance.entity.enums.EntryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Entity
@Table(name = "medical_entry")
public class MedicalEntry {
    @Id @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_record_id", nullable = false)
    private HealthRecord healthRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 30)
    private EntryType entryType;

    @Column(name = "diagnosis_code", length = 50)
    private String diagnosisCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "provider_name", length = 200)
    private String providerName;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
