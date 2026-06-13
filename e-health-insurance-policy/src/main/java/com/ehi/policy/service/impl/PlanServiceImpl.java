package com.ehi.policy.service.impl;

import com.ehi.infra.exception.DuplicateResourceException;
import com.ehi.infra.exception.NotFoundException;
import com.ehi.infra.exception.base.BadRequestException;
import com.ehi.policy.dto.request.CreatePlanRequest;
import com.ehi.policy.dto.response.PlanDto;
import com.ehi.policy.entity.Plan;
import com.ehi.policy.mapper.PlanMapper;
import com.ehi.policy.repository.PlanRepository;
import com.ehi.policy.repository.PolicyRepository;
import com.ehi.policy.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final PolicyRepository policyRepository;
    private final PlanMapper planMapper;

    @Override
    public List<PlanDto> getActivePlans() {
        return planRepository.findByActiveTrue().stream()
                .map(planMapper::toDto)
                .toList();
    }

    @Override
    public PlanDto getPlanById(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan", id));
        return planMapper.toDto(plan);
    }

    @Override
    public PlanDto createPlan(CreatePlanRequest request) {
        if (planRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Plan already exists: " + request.name());
        }

        Plan plan = Plan.builder()
                .name(request.name())
                .description(request.description())
                .coverageAmount(request.coverageAmount())
                .premiumAmount(request.premiumAmount())
                .durationMonths(request.durationMonths())
                .active(true)
                .build();

        PlanDto result = planMapper.toDto(planRepository.save(plan));
        log.info("Plan created: planId={}, name={}", result.id(), result.name());
        return result;
    }

    @Override
    public void deletePlan(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan", id));

        if (policyRepository.existsByPlan_Id(id)) {
            throw new BadRequestException("Cannot delete a plan that has existing policies");
        }

        planRepository.delete(plan);
        log.info("Plan deleted: planId={}, name={}", id, plan.getName());
    }
}
