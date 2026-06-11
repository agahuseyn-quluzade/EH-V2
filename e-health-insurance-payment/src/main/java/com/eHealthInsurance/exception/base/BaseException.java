package com.eHealthInsurance.exception.base;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final BaseErrorEnum errorEnum;
    private final Object[] args;

    protected BaseException(BaseErrorEnum errorEnum, Object... args) {
        super(errorEnum.getMessage());
        this.errorEnum = errorEnum;
        this.args = args;
    }

    protected BaseException(BaseErrorEnum errorEnum, Throwable cause, Object... args) {
        super(errorEnum.getMessage(), cause);
        this.errorEnum = errorEnum;
        this.args = args;
    }
}
