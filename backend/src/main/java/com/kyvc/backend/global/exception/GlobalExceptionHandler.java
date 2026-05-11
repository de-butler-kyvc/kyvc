package com.kyvc.backend.global.exception;

import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    // 비즈니스 예외 처리, 4xx warn / 5xx error 로그
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<CommonResponse<Object>> handleApiException(
            ApiException exception, // 비즈니스 예외
            HttpServletRequest request // 요청 정보
    ) {
        ErrorCode errorCode = exception.getErrorCode(); // 응답 상태와 코드 기준
        Map<String, Object> fields = buildExceptionFields(
                request,
                errorCode,
                exception.getClass().getName(),
                exception.getMessage()
        ); // 예외 로그 필드
        String event = resolveApiExceptionEvent(errorCode); // 예외 이벤트명

        if (errorCode.getStatus().is5xxServerError()) {
            logEventLogger.error(event, exception.getMessage(), fields, exception);
        } else {
            logEventLogger.warn(event, exception.getMessage(), fields);
        }

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponseFactory.fail(errorCode, exception.getMessage(), null));
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

        logValidationException(request, exception.getClass().getName());

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

        logValidationException(request, exception.getClass().getName());

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

        logValidationException(request, exception.getClass().getName());

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
        logValidationException(request, exception.getClass().getName());

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
                buildExceptionFields(request, errorCode, exception.getClass().getName(), errorCode.getMessage())
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
                buildExceptionFields(request, errorCode, exception.getClass().getName(), errorCode.getMessage())
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponseFactory.fail(errorCode));
    }

    // 예상하지 못한 예외 처리, error 로그
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<Object>> handleNoResourceFoundException(
            NoResourceFoundException exception, // 미매핑 리소스 예외
            HttpServletRequest request // 요청 정보
    ) {
        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        logEventLogger.warn(
                "exception.resource_not_found",
                errorCode.getMessage(),
                buildExceptionFields(request, errorCode, exception.getClass().getName(), errorCode.getMessage())
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponseFactory.fail(errorCode));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<CommonResponse<Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception, // 업로드 크기 초과 예외
            HttpServletRequest request // 요청 정보
    ) {
        ErrorCode errorCode = ErrorCode.DOCUMENT_SIZE_EXCEEDED;
        logEventLogger.warn(
                "exception.upload_size_exceeded",
                errorCode.getMessage(),
                buildExceptionFields(request, errorCode, exception.getClass().getName(), errorCode.getMessage())
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponseFactory.fail(errorCode));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<CommonResponse<Object>> handleDataAccessException(
            DataAccessException exception, // DB 접근 예외
            HttpServletRequest request // 요청 정보
    ) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        logEventLogger.error(
                "exception.database",
                errorCode.getMessage(),
                buildExceptionFields(request, errorCode, exception.getClass().getName(), errorCode.getMessage()),
                exception
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponseFactory.fail(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Object>> handleException(
            Exception exception, // 미처리 예외
            HttpServletRequest request // 요청 정보
    ) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        logEventLogger.error(
                "exception.unexpected",
                errorCode.getMessage(),
                buildExceptionFields(request, errorCode, exception.getClass().getName(), errorCode.getMessage()),
                exception
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponseFactory.fail(errorCode));
    }

    // 검증 예외 로그 출력
    private void logValidationException(
            HttpServletRequest request, // 요청 정보
            String exception // 예외 클래스명
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        logEventLogger.warn(
                "exception.validation",
                errorCode.getMessage(),
                buildExceptionFields(request, errorCode, exception, errorCode.getMessage())
        );
    }

    // 공통 예외 로그 필드 생성
    private Map<String, Object> buildExceptionFields(
            HttpServletRequest request, // 요청 정보
            ErrorCode errorCode, // 응답 상태와 코드 기준
            String exception, // 예외 클래스명
            String message // 예외 메시지
    ) {
        Map<String, Object> fields = new LinkedHashMap<>(); // 예외 로그 필드
        fields.put("path", request.getRequestURI());
        fields.put("method", request.getMethod());
        fields.put("status", errorCode.getStatus().value());
        fields.put("code", errorCode.getCode());
        fields.put("errorMessage", message);
        fields.put("exception", exception);
        fields.put("authenticatedUserId", resolveAuthenticatedUserId());
        return fields;
    }

    private String resolveApiExceptionEvent(ErrorCode errorCode // 오류 코드
    ) {
        String code = errorCode.getCode(); // 오류 코드 문자열
        if (code != null && code.contains("CORE")) {
            return "exception.core";
        }
        return "exception.api";
    }

    private Long resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // 현재 인증 정보
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal(); // 인증 주체
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
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
