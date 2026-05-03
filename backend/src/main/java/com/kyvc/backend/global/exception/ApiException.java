package com.kyvc.backend.global.exception;

import org.springframework.http.HttpStatus;

import java.util.Objects;

// 비즈니스 예외 공통 클래스
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    // 기본 메시지 기반 예외 생성
    public ApiException(ErrorCode errorCode) {
        this(errorCode, Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
    }

    // 사용자 지정 메시지 기반 예외 생성
    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    // 원인 예외 포함 예외 생성
    public ApiException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage(), cause);
    }

    // 사용자 지정 메시지와 원인 예외 포함 예외 생성
    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    // 에러 코드 조회
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    // HTTP 상태 조회
    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }
}
