package com.ehi.ai.repository;

import com.ehi.ai.entity.FraudCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudCheckRepository extends JpaRepository<FraudCheck, UUID> {

    Optional<FraudCheck> findByClaimId(UUID claimId);

    List<FraudCheck> findByUserId(UUID userId);
}
