package com.eHealthInsurance.exception;

import com.eHealthInsurance.exception.base.BaseException;
import com.eHealthInsurance.exception.base.BaseErrorEnum;

public class HealthRecordException extends BaseException {
    public HealthRecordException(BaseErrorEnum errorEnum, Object... args) {
        super(errorEnum, args);
    }
    public HealthRecordException(BaseErrorEnum errorEnum, Throwable cause, Object... args) {
        super(errorEnum, cause, args);
    }
}
