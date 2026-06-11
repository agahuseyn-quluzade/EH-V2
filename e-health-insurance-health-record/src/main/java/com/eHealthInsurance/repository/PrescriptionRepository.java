package com.eHealthInsurance.repository;
import com.eHealthInsurance.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    List<Prescription> findByHealthRecordIdOrderByPrescribedDateDesc(UUID healthRecordId);
}
