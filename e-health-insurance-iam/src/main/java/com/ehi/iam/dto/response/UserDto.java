package com.ehi.iam.dto.response;

import com.ehi.infra.enums.UserRole;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        Instant createdAt
) {
}
