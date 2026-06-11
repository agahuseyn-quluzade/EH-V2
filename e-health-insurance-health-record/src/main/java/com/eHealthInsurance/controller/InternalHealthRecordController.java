package com.eHealthInsurance.controller;

import com.eHealthInsurance.dto.response.HealthSummaryResponse;
import com.eHealthInsurance.service.HealthRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/records")
@RequiredArgsConstructor
public class InternalHealthRecordController {

    private final HealthRecordService healthRecordService;

    @GetMapping("/{memberId}/summary")
    public HealthSummaryResponse getSummary(@PathVariable UUID memberId) {
        return healthRecordService.getInternalSummary(memberId);
    }
}
