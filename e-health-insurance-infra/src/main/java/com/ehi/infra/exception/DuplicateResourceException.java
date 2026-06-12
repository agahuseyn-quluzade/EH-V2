package com.ehi.infra.exception;

import com.ehi.infra.exception.base.BaseErrorEnum;
import com.ehi.infra.exception.base.BaseException;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(BaseErrorEnum.DUPLICATE_RESOURCE, message);
    }
}
