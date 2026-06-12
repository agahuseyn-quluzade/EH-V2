package com.ehi.infra.exception.base;

public interface BaseErrorService {

    String getErrorCode();

    String getMessage();

    int getHttpStatus();
}
