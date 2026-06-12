package com.ehi.claim.service;

import com.ehi.claim.dto.request.ReviewClaimRequest;
import com.ehi.claim.dto.request.SubmitClaimRequest;
import com.ehi.claim.dto.response.ClaimDto;
import com.ehi.claim.dto.response.ClaimEvidenceDto;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.ClaimStatus;
import com.ehi.infra.event.FraudDetectedEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ClaimService {

    ClaimDto submitClaim(UUID userId, SubmitClaimRequest request);

    List<ClaimDto> getMyClaims(UUID userId);

    ClaimDto getClaimById(UUID claimId, UUID requesterId, boolean privileged);

    ClaimEvidenceDto uploadEvidence(UUID claimId, UUID requesterId, boolean privileged, MultipartFile file);

    PagedResponse<ClaimDto> getAllClaims(ClaimStatus status, Pageable pageable);

    ClaimDto reviewClaim(UUID claimId, UUID reviewerId, ReviewClaimRequest request);

    void applyFraudResult(FraudDetectedEvent event);
}
