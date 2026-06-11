package com.eHealthInsurance.exception;

import com.eHealthInsurance.exception.base.BaseErrorEnum;
import org.springframework.http.HttpStatus;

public enum HealthRecordErrorEnum implements BaseErrorEnum {
    RECORD_NOT_FOUND("HR-NOT-FOUND-001", "Health record not found for member", HttpStatus.NOT_FOUND),
    RECORD_ALREADY_EXISTS("HR-ALREADY-EXISTS-002", "Health record already exists for member", HttpStatus.CONFLICT),
    ENTRY_NOT_FOUND("HR-ENTRY-NOT-FOUND-003", "Medical entry not found", HttpStatus.NOT_FOUND),
    PRESCRIPTION_NOT_FOUND("HR-RX-NOT-FOUND-004", "Prescription not found", HttpStatus.NOT_FOUND),
    LAB_RESULT_NOT_FOUND("HR-LAB-NOT-FOUND-005", "Lab result not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_ACCESS("HR-UNAUTHORIZED-006", "Unauthorized access to health record", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    HealthRecordErrorEnum(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
    @Override public HttpStatus getHttpStatus() { return httpStatus; }
}
