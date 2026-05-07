package com.kyvc.backendadmin.global.exception;

/**
 * 필드별 검증 오류 응답 객체
 *
 * @param field 오류 발생 필드명
 * @param message 검증 실패 메시지
 */
public record ValidationErrorResponse(
        String field, // 오류 발생 필드명
        String message // 검증 실패 메시지
) {

    /**
     * @return 오류 발생 필드명
     */
    public String field() {
        return field;
    }

    /**
     * @return 검증 실패 메시지
     */
    public String message() {
        return message;
    }
}
