package com.eHealthInsurance.repository;
import com.eHealthInsurance.entity.MedicalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MedicalEntryRepository extends JpaRepository<MedicalEntry, UUID> {
    List<MedicalEntry> findByHealthRecordIdOrderByEntryDateDesc(UUID healthRecordId);
}
