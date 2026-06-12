package com.ehi.infra.exception;

import com.ehi.infra.exception.base.BaseErrorEnum;
import com.ehi.infra.exception.base.BaseException;

public class NotFoundException extends BaseException {

    public NotFoundException(String entity, Object id) {
        super(BaseErrorEnum.NOT_FOUND, entity + " not found with id: " + id);
    }
}
