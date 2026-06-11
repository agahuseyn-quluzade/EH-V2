package com.eHealthInsurance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Entity
@Table(name = "lab_result")
public class LabResult {
    @Id @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_record_id", nullable = false)
    private HealthRecord healthRecord;

    @Column(name = "test_name", nullable = false, length = 200)
    private String testName;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    @Column(name = "result_value", length = 100)
    private String resultValue;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(length = 50)
    private String unit;

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "COMPLETED";
    }
}
