package com.ehi.payment.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EpointCardRegistrationRequest(
        @JsonProperty("public_key") String publicKey,
        String language,
        Integer refund,
        String description,
        @JsonProperty("success_redirect_url") String successRedirectUrl,
        @JsonProperty("error_redirect_url") String errorRedirectUrl
) {
}
