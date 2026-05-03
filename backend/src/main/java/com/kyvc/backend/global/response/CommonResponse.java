package com.kyvc.backend.global.response;

/**
 * KYvC Backend API 표준 응답 형식
 *
 * @param success 요청 처리 성공 여부
 * @param code 응답 코드
 * @param message 응답 메시지
 * @param data 표준 응답 데이터
 * @param <T> 응답 데이터 타입
 */
public record CommonResponse<T>(
        boolean success, // 요청 처리 성공 여부
        String code, // 응답 코드
        String message, // 응답 메시지
        T data // 표준 응답 데이터
) {

    /**
     * @return 요청 처리 성공 여부
     */
    public boolean success() {
        return success;
    }

    /**
     * @return 응답 코드
     */
    public String code() {
        return code;
    }

    /**
     * @return 응답 메시지
     */
    public String message() {
        return message;
    }

    /**
     * @return 표준 응답 데이터
     */
    public T data() {
        return data;
    }
}
