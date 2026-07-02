package com.labreserve.handler;

import com.labreserve.dto.ApiError;
import com.labreserve.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException e) {
        HttpStatus status = switch (e.getCode()) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "USERNAME_EXISTS", "NAME_EXISTS", "SERIAL_NUMBER_EXISTS", "TIME_CONFLICT",
                 "ALREADY_REVIEWED", "EQUIPMENT_UNAVAILABLE", "ALREADY_PROCESSED",
                 "LOCK_FAILED" -> HttpStatus.CONFLICT;
            case "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "ACCOUNT_DISABLED", "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "WRONG_PASSWORD", "LAB_CLOSED", "OUTSIDE_OPEN_HOURS",
                 "BOOKING_NOT_COMPLETED", "BOOKING_NOT_CANCELLABLE", "BOOKING_NOT_EDITABLE",
                 "INVALID_STATUS", "INVALID_SEMESTER", "INVALID_DAY_OF_WEEK",
                 "INVALID_TIME_RANGE", "INVALID_DATE_RANGE", "CLASS_REQUIRED" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return ResponseEntity.status(status)
                .body(new ApiError(e.getCode(), e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> details = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                details.put(fe.getField(), fe.getDefaultMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VALIDATION_ERROR", "请求参数校验失败", details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("FORBIDDEN", "无权限", null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("BAD_REQUEST", "请求体格式错误，请检查 JSON 格式", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception e) {
        // Let Spring handle known HTTP exceptions (405, 415 etc.) natively
        if (e instanceof org.springframework.web.HttpRequestMethodNotSupportedException
                || e instanceof org.springframework.web.HttpMediaTypeNotSupportedException) {
            throw (RuntimeException) e;
        }
        log.error("Unhandled exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "服务器内部错误", null));
    }
}
