package com.eHealthInsurance.exception.base;
import org.springframework.http.HttpStatus;
public interface BaseErrorEnum { String getCode(); String getMessage(); HttpStatus getHttpStatus(); }
