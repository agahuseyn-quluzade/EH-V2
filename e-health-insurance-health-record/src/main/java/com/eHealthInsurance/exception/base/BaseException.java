package com.eHealthInsurance.exception.base;

public abstract class BaseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
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

    public BaseErrorEnum getErrorEnum() { return errorEnum; }
    public Object[] getArgs() { return args; }
}
