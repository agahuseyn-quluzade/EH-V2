package com.ehi.infra.exception.base;

public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(BaseErrorEnum.BAD_REQUEST, message);
    }
}
