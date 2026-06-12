package com.ehi.policy.exception;

import com.ehi.infra.exception.base.BaseErrorService;

public enum PolicyErrorEnum implements BaseErrorService {

    FORBIDDEN("POLICY-FORBIDDEN-0001", "Access denied", 403);

    private final String errorCode;
    private final String message;
    private final int httpStatus;

    PolicyErrorEnum(String errorCode, String message, int httpStatus) {
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
