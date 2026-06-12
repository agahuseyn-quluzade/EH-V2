package com.ehi.payment.exception;

import com.ehi.infra.exception.BaseErrorService;

public enum PaymentErrorEnum implements BaseErrorService {

    FORBIDDEN("PAYMENT-FORBIDDEN-0001", "Access denied", 403);

    private final String errorCode;
    private final String message;
    private final int httpStatus;

    PaymentErrorEnum(String errorCode, String message, int httpStatus) {
        this.errorCode = errorCode;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }
}
