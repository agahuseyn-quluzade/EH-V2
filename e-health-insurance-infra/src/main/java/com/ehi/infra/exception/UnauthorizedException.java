package com.ehi.infra.exception;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(BaseErrorEnum.UNAUTHORIZED, message);
    }
}
