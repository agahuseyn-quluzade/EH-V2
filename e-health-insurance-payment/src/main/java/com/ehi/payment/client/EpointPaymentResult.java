package com.ehi.payment.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record EpointPaymentResult(
        @JsonProperty("order_id") String orderId,
        String status,
        String code,
        String message,
        String transaction,
        @JsonProperty("bank_transaction") String bankTransaction,
        @JsonProperty("operation_code") String operationCode,
        String rrn,
        @JsonProperty("card_name") String cardName,
        @JsonProperty("card_mask") String cardMask,
        @JsonProperty("card_id") String cardId,
        BigDecimal amount
) {

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
