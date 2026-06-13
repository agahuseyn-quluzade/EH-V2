package com.ehi.payment.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EpointPaymentRequest(
        @JsonProperty("public_key") String publicKey,
        BigDecimal amount,
        String currency,
        String language,
        @JsonProperty("order_id") String orderId,
        String description,
        @JsonProperty("success_redirect_url") String successRedirectUrl,
        @JsonProperty("error_redirect_url") String errorRedirectUrl
) {
}
