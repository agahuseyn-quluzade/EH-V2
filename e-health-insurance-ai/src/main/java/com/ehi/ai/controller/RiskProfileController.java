package com.ehi.ai.controller;

import com.ehi.ai.dto.response.RiskAiResponse;
import com.ehi.ai.service.RiskProfileService;
import com.ehi.infra.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class RiskProfileController {

    private final RiskProfileService riskProfileService;

    @GetMapping("/risk-profile/{userId}")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RiskAiResponse>> getRiskProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(riskProfileService.getRiskProfile(userId)));
    }
}
