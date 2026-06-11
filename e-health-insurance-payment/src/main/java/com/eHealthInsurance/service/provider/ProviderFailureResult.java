package com.eHealthInsurance.service.provider;

public record ProviderFailureResult(
        String providerReference,
        String failureCode,
        String failureReason
) {
}
