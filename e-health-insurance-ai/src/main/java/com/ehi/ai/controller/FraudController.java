package com.ehi.ai.controller;

import com.ehi.ai.dto.response.FraudAiResponse;
import com.ehi.ai.service.FraudDetectionService;
import com.ehi.infra.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    @GetMapping("/fraud-checks/{claimId}")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FraudAiResponse>> getFraudCheck(@PathVariable UUID claimId) {
        return ResponseEntity.ok(ApiResponse.ok(fraudDetectionService.getFraudCheck(claimId)));
    }

    @PostMapping("/claims/{claimId}/analyze")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FraudAiResponse>> analyzeClaim(@PathVariable UUID claimId) {
        return ResponseEntity.ok(ApiResponse.ok(fraudDetectionService.reanalyzeClaim(claimId)));
    }
}
