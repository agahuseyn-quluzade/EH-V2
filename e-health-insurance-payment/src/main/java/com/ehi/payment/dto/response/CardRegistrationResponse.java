package com.ehi.payment.dto.response;

import java.util.UUID;

public record CardRegistrationResponse(
        UUID id,
        String redirectUrl
) {
}
