package com.eHealthInsurance.exception;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, String path, Instant timestamp, int status, Map<String, String> fieldErrors) {
    public ErrorResponse(String code, String message, String path, Instant timestamp, int status) { this(code, message, path, timestamp, status, null); }
}
