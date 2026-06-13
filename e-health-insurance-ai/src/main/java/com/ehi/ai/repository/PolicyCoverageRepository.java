package com.ehi.ai.repository;

import com.ehi.ai.entity.PolicyCoverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PolicyCoverageRepository extends JpaRepository<PolicyCoverage, UUID> {

    Optional<PolicyCoverage> findByPolicyId(UUID policyId);
}
