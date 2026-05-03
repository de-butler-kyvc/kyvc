package com.kyvc.backend.global.response;

import com.kyvc.backend.global.exception.ErrorCode;

import java.util.Objects;

// CommonResponse 생성 유틸
public final class CommonResponseFactory {

    private static final String SUCCESS_CODE = "SUCCESS";
    private static final String SUCCESS_MESSAGE = "Request completed successfully.";

    private CommonResponseFactory() {
    }

    // 데이터 포함 성공 응답 생성
    public static <T> CommonResponse<T> success(T data) {
        return success(SUCCESS_MESSAGE, data);
    }

    // 메시지 지정 성공 응답 생성
    public static <T> CommonResponse<T> success(String message, T data) {
        return new CommonResponse<>(true, SUCCESS_CODE, message, data);
    }

    // 데이터 미포함 성공 응답 생성
    public static CommonResponse<Void> successWithoutData() {
        return new CommonResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    // 기본 에러 메시지 실패 응답 생성
    public static CommonResponse<Object> fail(ErrorCode errorCode) {
        return fail(errorCode, null);
    }

    // 상세 데이터 포함 실패 응답 생성
    public static CommonResponse<Object> fail(ErrorCode errorCode, Object data) {
        ErrorCode resolvedErrorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        return fail(resolvedErrorCode, resolvedErrorCode.getMessage(), data);
    }

    // 메시지 지정 실패 응답 생성
    public static CommonResponse<Object> fail(ErrorCode errorCode, String message, Object data) {
        ErrorCode resolvedErrorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        return new CommonResponse<>(false, resolvedErrorCode.getCode(), message, data);
    }
}
