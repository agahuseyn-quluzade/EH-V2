package com.ehi.infra.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        int status,
        String message,
        Map<String, Object> details,
        Instant timestamp
) {
}
