package com.ehi.policy.repository;

import com.ehi.infra.enums.PolicyStatus;
import com.ehi.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    List<Policy> findByUserId(UUID userId);

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<PolicyStatus> statuses);

    boolean existsByPlan_Id(UUID planId);
}
