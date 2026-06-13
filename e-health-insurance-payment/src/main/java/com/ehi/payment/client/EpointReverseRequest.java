package com.ehi.payment.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EpointReverseRequest(
        @JsonProperty("public_key") String publicKey,
        String language,
        String transaction,
        String currency,
        BigDecimal amount
) {
}
