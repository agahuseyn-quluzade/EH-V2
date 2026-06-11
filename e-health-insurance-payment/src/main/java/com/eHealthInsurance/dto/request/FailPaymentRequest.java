package com.eHealthInsurance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FailPaymentRequest(
        String providerReference,
        String failureCode,
        @NotBlank String failureReason
) {
}
