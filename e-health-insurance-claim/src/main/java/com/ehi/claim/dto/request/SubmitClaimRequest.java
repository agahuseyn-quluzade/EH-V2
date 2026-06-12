package com.ehi.claim.dto.request;

import com.ehi.infra.enums.ClaimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record SubmitClaimRequest(
        @NotNull UUID policyId,
        @NotNull ClaimType claimType,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String description
) {
}
