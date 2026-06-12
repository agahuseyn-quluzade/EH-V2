package com.ehi.ai.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ChatResponse(
        UUID sessionId,
        String reply,
        Instant timestamp
) {
}
