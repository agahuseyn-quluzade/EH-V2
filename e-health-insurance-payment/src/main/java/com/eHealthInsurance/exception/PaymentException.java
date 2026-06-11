package com.eHealthInsurance.exception;

import com.eHealthInsurance.exception.base.BaseException;

public class PaymentException extends BaseException {

    public PaymentException(PaymentErrorEnum errorEnum) {
        super(errorEnum);
    }

    public PaymentException(PaymentErrorEnum errorEnum, Throwable cause) {
        super(errorEnum, cause);
    }
}
