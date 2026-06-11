package com.eHealthInsurance.exception;

import com.eHealthInsurance.exception.base.BaseErrorEnum;
import org.springframework.http.HttpStatus;

public enum PaymentErrorEnum implements BaseErrorEnum {

    PAYMENT_NOT_FOUND("PAYMENT_NOT_FOUND", "Payment not found", HttpStatus.NOT_FOUND),
    PAYMENT_ALREADY_CONFIRMED("PAYMENT_ALREADY_CONFIRMED", "Payment is already confirmed", HttpStatus.CONFLICT),
    PAYMENT_INVALID_AMOUNT("PAYMENT_INVALID_AMOUNT", "Payment amount must be greater than zero", HttpStatus.BAD_REQUEST),
    PAYMENT_INVALID_STATE("PAYMENT_INVALID_STATE", "Payment is not in a valid state for this operation", HttpStatus.CONFLICT),
    PAYMENT_PROVIDER_NOT_SUPPORTED("PAYMENT_PROVIDER_NOT_SUPPORTED", "Payment provider is not supported", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_REQUIRED("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header or request idempotencyKey is required", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_REQUEST_IN_PROGRESS("IDEMPOTENCY_REQUEST_IN_PROGRESS", "A payment request with this idempotency key is already in progress", HttpStatus.CONFLICT),
    INVOICE_NOT_FOUND("INVOICE_NOT_FOUND", "Invoice not found", HttpStatus.NOT_FOUND),
    REFUND_NOT_FOUND("REFUND_NOT_FOUND", "Refund not found", HttpStatus.NOT_FOUND),
    REFUND_ALREADY_PROCESSED("REFUND_ALREADY_PROCESSED", "Refund has already been processed", HttpStatus.CONFLICT),
    POLICY_NOT_FOUND("POLICY_NOT_FOUND", "Policy not found", HttpStatus.NOT_FOUND),
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "Member not found or inactive", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    PaymentErrorEnum(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
