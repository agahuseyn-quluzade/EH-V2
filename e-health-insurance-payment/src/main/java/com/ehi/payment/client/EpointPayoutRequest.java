package com.ehi.payment.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EpointPayoutRequest(
        @JsonProperty("public_key") String publicKey,
        String language,
        @JsonProperty("card_id") String cardId,
        @JsonProperty("order_id") String orderId,
        BigDecimal amount,
        String currency,
        String description
) {
}
