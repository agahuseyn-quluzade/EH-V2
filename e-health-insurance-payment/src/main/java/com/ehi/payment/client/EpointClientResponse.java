package com.ehi.payment.client;

import java.util.Map;

public record EpointClientResponse(
        Map<String, Object> payload,
        String rawData,
        String rawSignature
) {
}
