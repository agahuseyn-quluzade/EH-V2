package com.ehi.infra.event;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserRegisteredEvent(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phone
) {
}
