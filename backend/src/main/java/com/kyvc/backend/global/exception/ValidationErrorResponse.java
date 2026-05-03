package com.kyvc.backend.global.exception;

// 필드별 검증 오류 응답 객체
public record ValidationErrorResponse(
        String field, // 오류 발생 필드명
        String message // 검증 실패 메시지
) {
}
