package com.ehi.policy.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PurchasePolicyRequest(
        @NotNull UUID planId
) {
}
