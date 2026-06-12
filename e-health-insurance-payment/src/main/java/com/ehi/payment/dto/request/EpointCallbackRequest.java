package com.ehi.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EpointCallbackRequest(
        @NotBlank String data,
        @NotBlank String signature
) {
}
