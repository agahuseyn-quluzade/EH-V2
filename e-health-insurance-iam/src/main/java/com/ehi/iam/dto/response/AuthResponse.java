package com.ehi.iam.dto.response;

import com.ehi.infra.enums.UserRole;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AuthResponse(
        UUID userId,
        String email,
        UserRole role,
        String accessToken,
        String refreshToken
) {
}
