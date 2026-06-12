package com.ehi.claim.repository;

import com.ehi.claim.entity.ClaimEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimEvidenceRepository extends JpaRepository<ClaimEvidence, UUID> {

    List<ClaimEvidence> findByClaim_Id(UUID claimId);
}
