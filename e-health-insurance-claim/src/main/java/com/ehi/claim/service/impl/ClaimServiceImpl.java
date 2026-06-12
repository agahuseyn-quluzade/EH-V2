package com.ehi.claim.service.impl;

import com.ehi.claim.dto.request.ReviewClaimRequest;
import com.ehi.claim.dto.request.SubmitClaimRequest;
import com.ehi.claim.dto.response.ClaimDto;
import com.ehi.claim.dto.response.ClaimEvidenceDto;
import com.ehi.claim.entity.Claim;
import com.ehi.claim.entity.ClaimEvidence;
import com.ehi.claim.kafka.ClaimDecisionEventProducer;
import com.ehi.claim.kafka.ClaimSubmittedEventProducer;
import com.ehi.claim.mapper.ClaimEvidenceMapper;
import com.ehi.claim.mapper.ClaimMapper;
import com.ehi.claim.repository.ClaimEvidenceRepository;
import com.ehi.claim.repository.ClaimRepository;
import com.ehi.claim.service.ClaimService;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.ClaimStatus;
import com.ehi.infra.event.ClaimDecisionEvent;
import com.ehi.infra.event.ClaimSubmittedEvent;
import com.ehi.infra.event.FraudDetectedEvent;
import com.ehi.infra.exception.BadRequestException;
import com.ehi.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimEvidenceRepository claimEvidenceRepository;
    private final ClaimMapper claimMapper;
    private final ClaimEvidenceMapper claimEvidenceMapper;
    private final ClaimSubmittedEventProducer claimSubmittedEventProducer;
    private final ClaimDecisionEventProducer claimDecisionEventProducer;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public ClaimDto submitClaim(UUID userId, SubmitClaimRequest request) {
        Claim claim = Claim.builder()
                .claimNumber(generateClaimNumber())
                .userId(userId)
                .policyId(request.policyId())
                .claimType(request.claimType())
                .amount(request.amount())
                .description(request.description())
                .status(ClaimStatus.SUBMITTED)
                .build();

        claim = claimRepository.save(claim);

        claimSubmittedEventProducer.publish(ClaimSubmittedEvent.builder()
                .claimId(claim.getId())
                .userId(claim.getUserId())
                .policyId(claim.getPolicyId())
                .claimNumber(claim.getClaimNumber())
                .claimType(claim.getClaimType())
                .amount(claim.getAmount())
                .build());

        return claimMapper.toDto(claim);
    }

    @Override
    public List<ClaimDto> getMyClaims(UUID userId) {
        return claimRepository.findByUserId(userId).stream()
                .map(claimMapper::toDto)
                .toList();
    }

    @Override
    public ClaimDto getClaimById(UUID claimId, UUID requesterId, boolean privileged) {
        return claimMapper.toDto(findAccessibleClaim(claimId, requesterId, privileged));
    }

    @Override
    public ClaimEvidenceDto uploadEvidence(UUID claimId, UUID requesterId, boolean privileged, MultipartFile file) {
        Claim claim = findAccessibleClaim(claimId, requesterId, privileged);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path targetDir = Paths.get(uploadDir, claimId.toString());
        Path targetPath = targetDir.resolve(fileName);

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store evidence file", e);
        }

        ClaimEvidence evidence = ClaimEvidence.builder()
                .claim(claim)
                .fileName(file.getOriginalFilename())
                .filePath(targetPath.toString())
                .contentType(file.getContentType())
                .build();

        return claimEvidenceMapper.toDto(claimEvidenceRepository.save(evidence));
    }

    @Override
    public PagedResponse<ClaimDto> getAllClaims(ClaimStatus status, Pageable pageable) {
        Page<Claim> page = status != null
                ? claimRepository.findByStatus(status, pageable)
                : claimRepository.findAll(pageable);

        return PagedResponse.<ClaimDto>builder()
                .content(page.getContent().stream().map(claimMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public ClaimDto reviewClaim(UUID claimId, UUID reviewerId, ReviewClaimRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Claim", claimId));

        if (claim.getStatus() != ClaimStatus.SUBMITTED && claim.getStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new BadRequestException("Claim cannot be reviewed in status: " + claim.getStatus());
        }

        if (request.decision() != ClaimStatus.APPROVED && request.decision() != ClaimStatus.REJECTED) {
            throw new BadRequestException("Decision must be APPROVED or REJECTED");
        }

        if (request.decision() == ClaimStatus.APPROVED) {
            if (request.approvedAmount() == null) {
                throw new BadRequestException("approvedAmount is required when approving a claim");
            }
            if (request.approvedAmount().compareTo(BigDecimal.ZERO) <= 0
                    || request.approvedAmount().compareTo(claim.getAmount()) > 0) {
                throw new BadRequestException("approvedAmount must be between 0 and the claimed amount");
            }
        }

        if (request.decision() == ClaimStatus.REJECTED && request.rejectionReason() == null) {
            throw new BadRequestException("rejectionReason is required when rejecting a claim");
        }

        claim.setStatus(request.decision());
        claim.setApprovedAmount(request.approvedAmount());
        claim.setRejectionReason(request.rejectionReason());
        claim.setReviewedBy(reviewerId);

        claim = claimRepository.save(claim);

        claimDecisionEventProducer.publish(ClaimDecisionEvent.builder()
                .claimId(claim.getId())
                .userId(claim.getUserId())
                .policyId(claim.getPolicyId())
                .decision(claim.getStatus())
                .approvedAmount(claim.getApprovedAmount())
                .rejectionReason(claim.getRejectionReason())
                .reviewedBy(claim.getReviewedBy())
                .build());

        return claimMapper.toDto(claim);
    }

    @Override
    public void applyFraudResult(FraudDetectedEvent event) {
        Claim claim = claimRepository.findById(event.claimId())
                .orElseThrow(() -> new NotFoundException("Claim", event.claimId()));

        if (claim.getStatus() != ClaimStatus.SUBMITTED) {
            return; // already reviewed manually, don't overwrite
        }

        claim.setRiskScore(event.riskScore());
        claim.setFraudFlags(event.flags());
        claim.setAiExplanation(event.aiExplanation());

        int score = event.riskScore() != null ? event.riskScore() : 0;

        if (score < 40) {
            claim.setStatus(ClaimStatus.APPROVED);
            claim.setApprovedAmount(claim.getAmount());
        } else if (score >= 70) {
            claim.setStatus(ClaimStatus.REJECTED);
            claim.setRejectionReason("Avtomatik rədd: yüksək fırıldaqçılıq riski (bal: " + score + ")");
        } else {
            claim.setStatus(ClaimStatus.UNDER_REVIEW);
        }

        claim = claimRepository.save(claim);

        if (claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED) {
            claimDecisionEventProducer.publish(ClaimDecisionEvent.builder()
                    .claimId(claim.getId())
                    .userId(claim.getUserId())
                    .policyId(claim.getPolicyId())
                    .decision(claim.getStatus())
                    .approvedAmount(claim.getApprovedAmount())
                    .rejectionReason(claim.getRejectionReason())
                    .reviewedBy(null)
                    .build());
        }
    }

    private Claim findAccessibleClaim(UUID claimId, UUID requesterId, boolean privileged) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NotFoundException("Claim", claimId));

        if (!privileged && !claim.getUserId().equals(requesterId)) {
            throw new NotFoundException("Claim", claimId);
        }

        return claim;
    }

    private String generateClaimNumber() {
        return "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
