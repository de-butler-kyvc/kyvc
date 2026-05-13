package com.kyvc.backendadmin.global.exception;

import com.kyvc.backendadmin.global.logging.LogEventLogger;
import com.kyvc.backendadmin.global.jwt.TokenCookieUtil;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// Controller 예외 공통 처리기
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String DEFAULT_VALIDATION_MESSAGE = "Invalid value.";

    private final LogEventLogger logEventLogger;
    private final TokenCookieUtil tokenCookieUtil;

    // 비즈니스 예외 처리, 4xx warn / 5xx error 로그
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<CommonResponse<Object>> handleApiException(
            ApiException exception, // 비즈니스 예외
            HttpServletRequest request // 요청 정보
    ) {
        ErrorCode errorCode = exception.getErrorCode(); // 응답 상태와 코드 기준
        Map<String, Object> fields = buildExceptionFields(request, errorCode, null); // 예외 로그 필드

        if (errorCode.getStatus().is5xxServerError()) {
            logEventLogger.error("exception.api", exception.getMessage(), fields);
        } else {
            logEventLogger.warn("exception.api", exception.getMessage(), fields);
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(errorCode.getStatus());
        if (isAdminRefreshRequest(request)) {
            responseBuilder
                    .header(HttpHeaders.SET_COOKIE, tokenCookieUtil.deleteAccessTokenCookie().toString())
                    .header(HttpHeaders.SET_COOKIE, tokenCookieUtil.deleteRefreshTokenCookie().toString());
        }

        return responseBuilder.body(CommonResponseFactory.fail(errorCode, exception.getMessage(), null));
    }

    // 요청 본문 검증 예외 처리, warn 로그
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception, // 요청 본문 검증 예외
            HttpServletRequest request // 요청 정보
    ) {
        List<ValidationErrorResponse> errors = Stream.concat( // 검증 오류 응답 데이터
                        exception.getBindingResult().getFieldErrors().stream().map(this::toValidationError),
                        exception.getBindingResult().getGlobalErrors().stream().map(this::toValidationError)
                )
                .toList();

        logValidationException(request);

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(CommonResponseFactory.fail(ErrorCode.INVALID_REQUEST, errors));
    }

    // 파라미터 검증 예외 처리, warn 로그
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException exception, // 파라미터 검증 예외
            HttpServletRequest request // 요청 정보
    ) {
        List<ValidationErrorResponse> errors = exception.getConstraintViolations().stream() // 검증 오류 응답 데이터
                .map(this::toValidationError)
                .toList();

        logValidationException(request);

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(CommonResponseFactory.fail(ErrorCode.INVALID_REQUEST, errors));
    }

    // 필수 요청 파라미터 누락 예외 처리, warn 로그
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonResponse<Object>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception, // 필수 파라미터 누락 예외
            HttpServletRequest request // 요청 정보
    ) {
        List<ValidationErrorResponse> errors = List.of( // 누락 파라미터 응답 데이터
                new ValidationErrorResponse(
                        exception.getParameterName(),
                        "Required request parameter is missing."
                )
        );

        logValidationException(request);

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(CommonResponseFactory.fail(ErrorCode.INVALID_REQUEST, errors));
    }

    // 요청 본문 파싱 예외 처리, warn 로그
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception, // 요청 본문 파싱 예외
            HttpServletRequest request // 요청 정보
    ) {
        logValidationException(request);

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(CommonResponseFactory.fail(ErrorCode.INVALID_REQUEST));
    }

    // 인가 예외 처리, warn 로그
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonResponse<Object>> handleAccessDeniedException(
            AccessDeniedException exception, // 인가 예외
            HttpServletRequest request // 요청 정보
    ) {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;
        logEventLogger.warn(
                "exception.access_denied",
                errorCode.getMessage(),
                buildExceptionFields(request, errorCode, null)
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponseFactory.fail(errorCode));
    }

    // 인증 예외 처리, warn 로그
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonResponse<Object>> handleAuthenticationException(
            AuthenticationException exception, // 인증 예외
            HttpServletRequest request // 요청 정보
    ) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        logEventLogger.warn(
                "exception.authentication",
                errorCode.getMessage(),
                buildExceptionFields(request, errorCode, null)
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponseFactory.fail(errorCode));
    }

    // 예상하지 못한 예외 처리, error 로그
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Object>> handleException(
            Exception exception, // 미처리 예외
            HttpServletRequest request // 요청 정보
    ) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        logEventLogger.error(
                "exception.unexpected",
                errorCode.getMessage(),
                buildExceptionFields(request, errorCode, exception.getClass().getName())
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponseFactory.fail(errorCode));
    }

    // 검증 예외 로그 출력
    private void logValidationException(HttpServletRequest request // 요청 정보
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        logEventLogger.warn(
                "exception.validation",
                errorCode.getMessage(),
                buildExceptionFields(request, errorCode, null)
        );
    }

    // 공통 예외 로그 필드 생성
    private Map<String, Object> buildExceptionFields(
            HttpServletRequest request, // 요청 정보
            ErrorCode errorCode, // 응답 상태와 코드 기준
            String exception // 예외 클래스명
    ) {
        Map<String, Object> fields = new LinkedHashMap<>(); // 예외 로그 필드
        fields.put("path", request.getRequestURI());
        fields.put("status", errorCode.getStatus().value());
        fields.put("code", errorCode.getCode());
        fields.put("exception", exception);
        return fields;
    }

    private boolean isAdminRefreshRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "POST".equalsIgnoreCase(request.getMethod())
                && ("/api/admin/auth/refresh".equals(path) || "/api/admin/auth/token/refresh".equals(path));
    }

    // 필드 검증 오류 응답 변환
    private ValidationErrorResponse toValidationError(FieldError error // 필드 검증 오류
    ) {
        return new ValidationErrorResponse(error.getField(), resolveMessage(error));
    }

    // 객체 검증 오류 응답 변환
    private ValidationErrorResponse toValidationError(ObjectError error // 객체 검증 오류
    ) {
        return new ValidationErrorResponse(error.getObjectName(), resolveMessage(error));
    }

    // 제약 조건 오류 응답 변환
    private ValidationErrorResponse toValidationError(ConstraintViolation<?> violation // 제약 조건 오류
    ) {
        return new ValidationErrorResponse(violation.getPropertyPath().toString(), violation.getMessage());
    }

    // 기본 검증 메시지 보정
    private String resolveMessage(ObjectError error // 검증 오류
    ) {
        String defaultMessage = error.getDefaultMessage(); // 검증 메시지 원문
        if (defaultMessage == null || defaultMessage.isBlank()) {
            return DEFAULT_VALIDATION_MESSAGE;
        }
        return defaultMessage;
    }
}
