package com.ehi.infra.exception;

public interface BaseErrorService {

    String getErrorCode();

    String getMessage();

    int getHttpStatus();
}
