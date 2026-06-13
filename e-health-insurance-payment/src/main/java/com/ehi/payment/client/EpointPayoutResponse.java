package com.ehi.payment.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record EpointPayoutResponse(
        String status,
        String transaction,
        @JsonProperty("bank_transaction") String bankTransaction,
        String rrn,
        @JsonProperty("card_mask") String cardMask,
        @JsonProperty("card_name") String cardName,
        BigDecimal amount,
        String message
) {

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
