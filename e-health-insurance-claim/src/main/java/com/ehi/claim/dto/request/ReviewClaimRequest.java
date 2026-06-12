package com.ehi.claim.dto.request;

import com.ehi.infra.enums.ClaimStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReviewClaimRequest(
        @NotNull ClaimStatus decision,
        BigDecimal approvedAmount,
        String rejectionReason
) {
}
