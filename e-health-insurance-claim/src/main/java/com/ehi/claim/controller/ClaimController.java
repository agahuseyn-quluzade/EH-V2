package com.ehi.claim.controller;

import com.ehi.claim.dto.request.ReviewClaimRequest;
import com.ehi.claim.dto.request.SubmitClaimRequest;
import com.ehi.claim.dto.response.ClaimDto;
import com.ehi.claim.dto.response.ClaimEvidenceDto;
import com.ehi.claim.service.ClaimService;
import com.ehi.infra.dto.ApiResponse;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.ClaimStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ClaimDto>> submitClaim(Authentication authentication,
                                                              @Valid @RequestBody SubmitClaimRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(claimService.submitClaim(userId, request)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<ClaimDto>>> getMyClaims(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(claimService.getMyClaims(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClaimDto>> getClaimById(Authentication authentication, @PathVariable UUID id) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(claimService.getClaimById(id, userId, isStaff(authentication))));
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<ApiResponse<ClaimEvidenceDto>> uploadEvidence(Authentication authentication,
                                                                          @PathVariable UUID id,
                                                                          @RequestParam("file") MultipartFile file) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(claimService.uploadEvidence(id, userId, isStaff(authentication), file)));
    }

    @GetMapping
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<ClaimDto>>> getAllClaims(
            @RequestParam(required = false) ClaimStatus status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(claimService.getAllClaims(status, pageable)));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ClaimDto>> reviewClaim(Authentication authentication,
                                                              @PathVariable UUID id,
                                                              @Valid @RequestBody ReviewClaimRequest request) {
        UUID reviewerId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(claimService.reviewClaim(id, reviewerId, request)));
    }

    private boolean isStaff(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_STAFF") || authority.equals("ROLE_ADMIN"));
    }
}
