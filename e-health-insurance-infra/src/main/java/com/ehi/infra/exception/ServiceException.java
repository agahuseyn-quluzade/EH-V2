package com.ehi.infra.exception;

import com.ehi.infra.exception.base.BaseErrorService;
import com.ehi.infra.exception.base.BaseException;

public class ServiceException extends BaseException {

    public ServiceException(BaseErrorService errorService) {
        super(errorService);
    }

    public ServiceException(BaseErrorService errorService, String message) {
        super(errorService, message);
    }
}
