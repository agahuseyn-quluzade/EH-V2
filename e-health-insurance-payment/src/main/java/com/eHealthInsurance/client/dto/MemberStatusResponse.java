package com.eHealthInsurance.client.dto;

import java.util.UUID;

public record MemberStatusResponse(
    UUID id,
    String email,
    boolean active
) {}
