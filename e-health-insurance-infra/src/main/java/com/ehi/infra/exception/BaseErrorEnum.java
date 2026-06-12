package com.ehi.infra.exception;

public enum BaseErrorEnum implements BaseErrorService {

    NOT_FOUND("BASE-NOT-FOUND-0001", "Resource not found", 404),
    UNAUTHORIZED("BASE-UNAUTHORIZED-0002", "Unauthorized", 401),
    BAD_REQUEST("BASE-BAD-REQUEST-0003", "Bad request", 400),
    DUPLICATE_RESOURCE("BASE-DUPLICATE-RESOURCE-0004", "Resource already exists", 409),
    VALIDATION_ERROR("BASE-VALIDATION-ERROR-0005", "Validation failed", 400),
    INTERNAL_ERROR("BASE-INTERNAL-ERROR-0006", "Internal server error", 500);

    private final String errorCode;
    private final String message;
    private final int httpStatus;

    BaseErrorEnum(String errorCode, String message, int httpStatus) {
        this.errorCode = errorCode;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }
}
