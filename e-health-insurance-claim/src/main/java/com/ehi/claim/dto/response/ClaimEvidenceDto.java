package com.ehi.claim.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ClaimEvidenceDto(
        UUID id,
        UUID claimId,
        String fileName,
        String filePath,
        String contentType,
        Instant uploadedAt
) {
}
