package com.ehi.infra.exception;

public class ServiceException extends BaseException {

    public ServiceException(BaseErrorService errorService) {
        super(errorService);
    }

    public ServiceException(BaseErrorService errorService, String message) {
        super(errorService, message);
    }
}
