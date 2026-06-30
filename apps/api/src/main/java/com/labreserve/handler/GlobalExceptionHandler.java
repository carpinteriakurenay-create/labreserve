package com.labreserve.handler;

import com.labreserve.dto.ApiError;
import com.labreserve.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException e) {
        HttpStatus status = switch (e.getCode()) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "USERNAME_EXISTS", "NAME_EXISTS", "SERIAL_NUMBER_EXISTS", "TIME_CONFLICT",
                 "ALREADY_REVIEWED", "EQUIPMENT_UNAVAILABLE", "ALREADY_PROCESSED" -> HttpStatus.CONFLICT;
            case "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "ACCOUNT_DISABLED", "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "WRONG_PASSWORD", "LAB_CLOSED", "OUTSIDE_OPEN_HOURS",
                 "BOOKING_NOT_COMPLETED", "BOOKING_NOT_CANCELLABLE", "BOOKING_NOT_EDITABLE" -> HttpStatus.BAD_REQUEST;
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
}
