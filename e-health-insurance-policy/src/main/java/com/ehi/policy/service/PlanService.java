package com.ehi.policy.service;

import com.ehi.policy.dto.request.CreatePlanRequest;
import com.ehi.policy.dto.response.PlanDto;

import java.util.List;
import java.util.UUID;

public interface PlanService {

    List<PlanDto> getActivePlans();

    PlanDto getPlanById(UUID id);

    PlanDto createPlan(CreatePlanRequest request);
}
