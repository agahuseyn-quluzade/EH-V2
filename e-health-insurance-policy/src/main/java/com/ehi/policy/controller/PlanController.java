package com.ehi.policy.controller;

import com.ehi.policy.dto.request.CreatePlanRequest;
import com.ehi.policy.dto.response.PlanDto;
import com.ehi.policy.service.PlanService;
import com.ehi.infra.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanDto>>> getActivePlans() {
        return ResponseEntity.ok(ApiResponse.ok(planService.getActivePlans()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanDto>> getPlanById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(planService.getPlanById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PlanDto>> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(planService.createPlan(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable UUID id) {
        planService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
