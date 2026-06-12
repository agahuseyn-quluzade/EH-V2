package com.ehi.ai.exception;

import com.ehi.infra.dto.ApiResponse;
import com.ehi.infra.dto.ErrorResponse;
import com.ehi.infra.exception.base.BaseErrorEnum;
import com.ehi.infra.exception.base.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBaseException(BaseException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return buildResponse(ex.getStatusCode(), ex.getMessage(), Map.of("errorCode", ex.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("errorCode", BaseErrorEnum.VALIDATION_ERROR.getErrorCode());
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));

        return buildResponse(BaseErrorEnum.VALIDATION_ERROR.getHttpStatus(), BaseErrorEnum.VALIDATION_ERROR.getMessage(), details);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAccessDeniedException(AccessDeniedException ex) {
        return buildResponse(AiErrorEnum.FORBIDDEN.getHttpStatus(), AiErrorEnum.FORBIDDEN.getMessage(),
                Map.of("errorCode", AiErrorEnum.FORBIDDEN.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return buildResponse(BaseErrorEnum.INTERNAL_ERROR.getHttpStatus(), BaseErrorEnum.INTERNAL_ERROR.getMessage(),
                Map.of("errorCode", BaseErrorEnum.INTERNAL_ERROR.getErrorCode()));
    }

    private ResponseEntity<ApiResponse<ErrorResponse>> buildResponse(int status, String message, Map<String, Object> details) {
        ErrorResponse errorResponse = new ErrorResponse(status, message, details, Instant.now());
        return ResponseEntity.status(status).body(new ApiResponse<>(false, errorResponse, message, Instant.now()));
    }
}
