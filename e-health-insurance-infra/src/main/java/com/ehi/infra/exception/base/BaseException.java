package com.ehi.infra.exception.base;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final BaseErrorService errorService;

    protected BaseException(BaseErrorService errorService, String message) {
        super(message);
        this.errorService = errorService;
    }

    protected BaseException(BaseErrorService errorService) {
        this(errorService, errorService.getMessage());
    }

    public String getErrorCode() {
        return errorService.getErrorCode();
    }

    public int getStatusCode() {
        return errorService.getHttpStatus();
    }
}
