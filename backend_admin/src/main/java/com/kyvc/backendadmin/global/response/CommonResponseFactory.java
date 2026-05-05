package com.kyvc.backendadmin.global.response;

import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

// CommonResponse 생성 유틸
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommonResponseFactory {

    private static final String SUCCESS_CODE = "OK";
    private static final String SUCCESS_MESSAGE = "요청이 성공했습니다.";

    // 데이터 포함 성공 응답 생성
    public static <T> CommonResponse<T> success(T data // 응답 데이터
    ) {
        return success(SUCCESS_MESSAGE, data);
    }

    // 메시지 지정 성공 응답 생성
    public static <T> CommonResponse<T> success(
            String message, // 응답 메시지
            T data // 응답 데이터
    ) {
        return new CommonResponse<>(true, SUCCESS_CODE, message, data);
    }

    // 데이터 미포함 성공 응답 생성
    public static CommonResponse<Void> successWithoutData() {
        return new CommonResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    // 기본 에러 메시지 실패 응답 생성
    public static CommonResponse<Object> fail(ErrorCode errorCode // 실패 상황 에러 코드
    ) {
        return fail(errorCode, null);
    }

    // 상세 데이터 포함 실패 응답 생성
    public static CommonResponse<Object> fail(
            ErrorCode errorCode, // 실패 상황 에러 코드
            Object data // 실패 상세 데이터
    ) {
        ErrorCode resolvedErrorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        return fail(resolvedErrorCode, resolvedErrorCode.getMessage(), data);
    }

    // 메시지 지정 실패 응답 생성
    public static CommonResponse<Object> fail(
            ErrorCode errorCode, // 실패 상황 에러 코드
            String message, // 응답 메시지
            Object data // 실패 상세 데이터
    ) {
        ErrorCode resolvedErrorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        return new CommonResponse<>(false, resolvedErrorCode.getCode(), message, data);
    }
}
