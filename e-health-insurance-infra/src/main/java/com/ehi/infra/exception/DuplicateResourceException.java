package com.ehi.infra.exception;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(BaseErrorEnum.DUPLICATE_RESOURCE, message);
    }
}
