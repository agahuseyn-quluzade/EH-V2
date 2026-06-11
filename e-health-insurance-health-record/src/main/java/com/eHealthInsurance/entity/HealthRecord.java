package com.eHealthInsurance.entity;

import com.eHealthInsurance.entity.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.*;

@Getter @Setter @Entity
@Table(name = "health_record")
public class HealthRecord {
    @Id @UuidGenerator
    private UUID id;

    @Column(name = "member_id", nullable = false, unique = true)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "healthRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MedicalEntry> medicalEntries = new LinkedHashSet<>();

    @OneToMany(mappedBy = "healthRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Prescription> prescriptions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "healthRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LabResult> labResults = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = RecordStatus.ACTIVE;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
