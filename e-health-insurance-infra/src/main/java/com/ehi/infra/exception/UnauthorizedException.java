package com.ehi.infra.exception;

import com.ehi.infra.exception.base.BaseErrorEnum;
import com.ehi.infra.exception.base.BaseException;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(BaseErrorEnum.UNAUTHORIZED, message);
    }
}
