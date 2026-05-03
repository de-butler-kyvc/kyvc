package com.kyvc.backend.global.response;

// KYvC Backend API 표준 응답 형식
public record CommonResponse<T>(
        boolean success, // 요청 처리 성공 여부
        String code, // 응답 코드
        String message, // 응답 메시지
        T data // 표준 응답 데이터
) {
}
