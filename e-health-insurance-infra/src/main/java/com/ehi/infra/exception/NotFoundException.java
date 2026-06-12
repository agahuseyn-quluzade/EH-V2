package com.ehi.infra.exception;

public class NotFoundException extends BaseException {

    public NotFoundException(String entity, Object id) {
        super(BaseErrorEnum.NOT_FOUND, entity + " not found with id: " + id);
    }
}
