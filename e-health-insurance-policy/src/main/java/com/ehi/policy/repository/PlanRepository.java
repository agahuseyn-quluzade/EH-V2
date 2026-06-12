package com.ehi.policy.repository;

import com.ehi.policy.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    List<Plan> findByActiveTrue();

    boolean existsByName(String name);
}
