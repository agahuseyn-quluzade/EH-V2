package com.ehi.payment.exception;

import com.ehi.infra.exception.base.BaseErrorService;

public enum PaymentErrorEnum implements BaseErrorService {

    FORBIDDEN("PAYMENT-FORBIDDEN-0001", "Access denied", 403),
    EPOINT_INVALID_SIGNATURE("PAYMENT-EPOINT-SIGNATURE-0002", "Invalid Epoint callback signature", 401),
    EPOINT_INVALID_PAYLOAD("PAYMENT-EPOINT-PAYLOAD-0003", "Invalid Epoint callback payload", 400),
    EPOINT_NO_TRANSACTION("PAYMENT-EPOINT-NO-TRANSACTION-0004", "Payment has no Epoint transaction", 400),
    EPOINT_CARD_REGISTRATION_FAILED("PAYMENT-EPOINT-CARD-0005", "Epoint card registration failed", 502);

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
