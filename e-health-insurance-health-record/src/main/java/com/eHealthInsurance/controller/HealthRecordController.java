package com.eHealthInsurance.controller;

import com.eHealthInsurance.dto.request.*;
import com.eHealthInsurance.dto.response.*;
import com.eHealthInsurance.service.HealthRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    @GetMapping("/me")
    public HealthSummaryResponse getMyRecord(Authentication authentication) {
        return healthRecordService.getHealthRecord(UUID.fromString(authentication.getName()));
    }

    @PostMapping("/me")
    public ResponseEntity<HealthSummaryResponse> createMyRecord(Authentication authentication) {
        return ResponseEntity.ok(healthRecordService.createHealthRecord(UUID.fromString(authentication.getName())));
    }

    @PatchMapping("/me/status")
    public ResponseEntity<HealthSummaryResponse> updateMyRecordStatus(
            Authentication authentication, @Valid @RequestBody UpdateHealthRecordStatusRequest request) {
        return ResponseEntity.ok(healthRecordService.updateHealthRecordStatus(
                UUID.fromString(authentication.getName()), request.status()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyRecord(Authentication authentication) {
        healthRecordService.deleteHealthRecord(UUID.fromString(authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/entries")
    public ResponseEntity<MedicalEntryResponse> addEntry(
            Authentication authentication, @Valid @RequestBody AddMedicalEntryRequest request) {
        return ResponseEntity.ok(healthRecordService.addMedicalEntry(UUID.fromString(authentication.getName()), request));
    }

    @GetMapping("/me/entries")
    public ResponseEntity<List<MedicalEntryResponse>> getEntries(Authentication authentication) {
        return ResponseEntity.ok(healthRecordService.getMedicalEntries(UUID.fromString(authentication.getName())));
    }

    @PostMapping("/me/prescriptions")
    public ResponseEntity<PrescriptionResponse> addPrescription(
            Authentication authentication, @Valid @RequestBody AddPrescriptionRequest request) {
        return ResponseEntity.ok(healthRecordService.addPrescription(UUID.fromString(authentication.getName()), request));
    }

    @GetMapping("/me/prescriptions")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptions(Authentication authentication) {
        return ResponseEntity.ok(healthRecordService.getPrescriptions(UUID.fromString(authentication.getName())));
    }

    @PostMapping("/me/lab-results")
    public ResponseEntity<LabResultResponse> addLabResult(
            Authentication authentication, @Valid @RequestBody AddLabResultRequest request) {
        return ResponseEntity.ok(healthRecordService.addLabResult(UUID.fromString(authentication.getName()), request));
    }

    @GetMapping("/me/lab-results")
    public ResponseEntity<List<LabResultResponse>> getLabResults(Authentication authentication) {
        return ResponseEntity.ok(healthRecordService.getLabResults(UUID.fromString(authentication.getName())));
    }

    @GetMapping("/members/{memberId}")
    public HealthSummaryResponse getMemberRecord(@PathVariable UUID memberId) {
        return healthRecordService.getHealthRecord(memberId);
    }

    @PatchMapping("/members/{memberId}/status")
    public ResponseEntity<HealthSummaryResponse> updateMemberRecordStatus(
            @PathVariable UUID memberId, @Valid @RequestBody UpdateHealthRecordStatusRequest request) {
        return ResponseEntity.ok(healthRecordService.updateHealthRecordStatus(memberId, request.status()));
    }

    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<Void> deleteMemberRecord(@PathVariable UUID memberId) {
        healthRecordService.deleteHealthRecord(memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/members/{memberId}/entries")
    public ResponseEntity<List<MedicalEntryResponse>> getMemberEntries(@PathVariable UUID memberId) {
        return ResponseEntity.ok(healthRecordService.getMedicalEntries(memberId));
    }

    @GetMapping("/members/{memberId}/prescriptions")
    public ResponseEntity<List<PrescriptionResponse>> getMemberPrescriptions(@PathVariable UUID memberId) {
        return ResponseEntity.ok(healthRecordService.getPrescriptions(memberId));
    }

    @GetMapping("/members/{memberId}/lab-results")
    public ResponseEntity<List<LabResultResponse>> getMemberLabResults(@PathVariable UUID memberId) {
        return ResponseEntity.ok(healthRecordService.getLabResults(memberId));
    }
}
