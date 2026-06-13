package com.ehi.payment.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record EpointCheckoutResponse(
        String status,
        String transaction,
        @JsonProperty("redirect_url") String redirectUrl,
        @JsonProperty("card_id") String cardId,
        String message
) {

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
