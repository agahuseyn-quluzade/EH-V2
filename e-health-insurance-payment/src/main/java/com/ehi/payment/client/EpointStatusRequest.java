package com.ehi.payment.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EpointStatusRequest(
        @JsonProperty("public_key") String publicKey,
        String transaction
) {
}
