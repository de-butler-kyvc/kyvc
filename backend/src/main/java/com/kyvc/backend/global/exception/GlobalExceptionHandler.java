package com.kyvc.backend.global.exception;

import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Stream;

// Controller 예외 공통 처리기
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DEFAULT_VALIDATION_MESSAGE = "Invalid value.";

    // 비즈니스 예외 처리, 4xx warn / 5xx error 로그
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<CommonResponse<Object>> handleApiException(ApiException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        if (errorCode.getStatus().is5xxServerError()) {
            log.error("API exception occurred: {}", exception.getMessage(), exception);
        } else {
            log.warn("API exception occurred: {}", exception.getMessage());
        }

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponseFactory.fail(errorCode, exception.getMessage(), null));
    }

    // Request Body 검증 예외 처리, warn 로그
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        List<ValidationErrorResponse> errors = Stream.concat(
                        exception.getBindingResult().getFieldErrors().stream().map(this::toValidationError),
                        exception.getBindingResult().getGlobalErrors().stream().map(this::toValidationError)
                )
                .toList();

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(CommonResponseFactory.fail(ErrorCode.INVALID_REQUEST, errors));
    }

    // 파라미터 검증 예외 처리, warn 로그
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        List<ValidationErrorResponse> errors = exception.getConstraintViolations().stream()
                .map(this::toValidationError)
                .toList();

        log.warn("Constraint validation failed: {}", errors);
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(CommonResponseFactory.fail(ErrorCode.INVALID_REQUEST, errors));
    }

    // 필수 요청 파라미터 누락 예외 처리, warn 로그
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonResponse<Object>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception
    ) {
        List<ValidationErrorResponse> errors = List.of(
                new ValidationErrorResponse(
                        exception.getParameterName(),
                        "Required request parameter is missing."
                )
        );

        log.warn("Required request parameter is missing: {}", exception.getParameterName());
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(CommonResponseFactory.fail(ErrorCode.INVALID_REQUEST, errors));
    }

    // 요청 본문 파싱 예외 처리, warn 로그
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        log.warn("HTTP message is not readable: {}", exception.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(CommonResponseFactory.fail(ErrorCode.INVALID_REQUEST));
    }

    // 인가 예외 처리, warn 로그
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonResponse<Object>> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.getStatus())
                .body(CommonResponseFactory.fail(ErrorCode.FORBIDDEN));
    }

    // 인증 예외 처리, warn 로그
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonResponse<Object>> handleAuthenticationException(AuthenticationException exception) {
        log.warn("Authentication failed: {}", exception.getMessage());
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getStatus())
                .body(CommonResponseFactory.fail(ErrorCode.UNAUTHORIZED));
    }

    // 예상하지 못한 예외 처리, error 로그
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Object>> handleException(Exception exception) {
        log.error("Unexpected exception occurred.", exception);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(CommonResponseFactory.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    private ValidationErrorResponse toValidationError(FieldError error) {
        return new ValidationErrorResponse(error.getField(), resolveMessage(error));
    }

    private ValidationErrorResponse toValidationError(ObjectError error) {
        return new ValidationErrorResponse(error.getObjectName(), resolveMessage(error));
    }

    private ValidationErrorResponse toValidationError(ConstraintViolation<?> violation) {
        return new ValidationErrorResponse(violation.getPropertyPath().toString(), violation.getMessage());
    }

    private String resolveMessage(ObjectError error) {
        String defaultMessage = error.getDefaultMessage();
        if (defaultMessage == null || defaultMessage.isBlank()) {
            return DEFAULT_VALIDATION_MESSAGE;
        }
        return defaultMessage;
    }
}
