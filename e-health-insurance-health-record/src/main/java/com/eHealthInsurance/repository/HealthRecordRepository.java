package com.eHealthInsurance.repository;
import com.eHealthInsurance.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {
    Optional<HealthRecord> findByMemberId(UUID memberId);
    boolean existsByMemberId(UUID memberId);
}
