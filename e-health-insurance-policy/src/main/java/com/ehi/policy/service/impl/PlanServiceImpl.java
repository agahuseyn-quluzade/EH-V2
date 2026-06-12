package com.ehi.policy.service.impl;

import com.ehi.infra.exception.DuplicateResourceException;
import com.ehi.infra.exception.NotFoundException;
import com.ehi.policy.dto.request.CreatePlanRequest;
import com.ehi.policy.dto.response.PlanDto;
import com.ehi.policy.entity.Plan;
import com.ehi.policy.mapper.PlanMapper;
import com.ehi.policy.repository.PlanRepository;
import com.ehi.policy.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
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

        return planMapper.toDto(planRepository.save(plan));
    }
}
