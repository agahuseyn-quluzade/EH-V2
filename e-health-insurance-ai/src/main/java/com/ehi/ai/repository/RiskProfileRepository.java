package com.ehi.ai.repository;

import com.ehi.ai.entity.RiskProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiskProfileRepository extends JpaRepository<RiskProfile, UUID> {

    Optional<RiskProfile> findByUserId(UUID userId);
}
