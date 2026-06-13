package com.ehi.payment.client;

public record EpointReverseResponse(
        String status,
        String message
) {

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
