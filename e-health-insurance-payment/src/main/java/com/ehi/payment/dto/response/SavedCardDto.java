package com.ehi.payment.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record SavedCardDto(
        UUID id,
        String cardMask,
        String cardName,
        boolean active,
        Instant createdAt
) {
}
