package com.kyvc.backendadmin.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// KYvC Backend API 공통 에러 코드
@Getter
public enum ErrorCode {

    // HTTP 400 - 잘못된 요청 파라미터, 요청 본문, 검증 오류
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    // HTTP 401 - 인증 정보 누락 또는 미인증 접근
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    // HTTP 403 - 권한 없는 리소스 또는 기능 접근
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    // HTTP 404 - 공통 리소스 조회 실패
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    // HTTP 409 - 중복 리소스 생성 시도
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 존재하는 리소스입니다."),
    // HTTP 409 - 허용되지 않은 리소스 상태
    INVALID_STATUS(HttpStatus.CONFLICT, "INVALID_STATUS", "허용되지 않은 상태입니다."),
    // HTTP 500 - 파일 업로드 처리 실패
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_UPLOAD_FAILED", "파일 업로드에 실패했습니다."),
    // HTTP 500 - 분류되지 않은 서버 내부 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // HTTP 401 - 로그인 인증 실패
    AUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_LOGIN_FAILED", "로그인에 실패했습니다."),
    // HTTP 401 - 인증 토큰 만료
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_EXPIRED", "인증 토큰이 만료되었습니다."),
    // HTTP 401 - 인증 토큰 형식 또는 서명 오류
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_INVALID", "인증 토큰이 유효하지 않습니다."),
    // HTTP 401 - Refresh Token 저장 이력 조회 실패
    AUTH_REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_NOT_FOUND", "리프레시 토큰을 찾을 수 없습니다."),
    // HTTP 401 - 폐기된 Refresh Token 사용 시도
    AUTH_REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_REVOKED", "리프레시 토큰이 폐기되었습니다."),
    // HTTP 401 - 기대한 토큰 유형과 다른 토큰 사용
    AUTH_INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_TOKEN_TYPE", "토큰 유형이 올바르지 않습니다."),
    // HTTP 404 - 사용자 정보 조회 실패
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    // HTTP 409 - 이미 등록된 사용자 정보 재생성 시도
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "이미 존재하는 사용자입니다."),
    // HTTP 403 - 로그인 또는 인증 불가 사용자 상태
    USER_INACTIVE(HttpStatus.FORBIDDEN, "USER_INACTIVE", "비활성 사용자입니다."),
    // HTTP 404 - 기업 정보 조회 실패
    CORPORATE_NOT_FOUND(HttpStatus.NOT_FOUND, "CORPORATE_NOT_FOUND", "기업 정보를 찾을 수 없습니다."),
    // HTTP 403 - 기업 리소스 접근 권한 없음
    CORPORATE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CORPORATE_ACCESS_DENIED", "기업 접근 권한이 없습니다."),
    // HTTP 404 - KYC 정보 조회 실패
    KYC_NOT_FOUND(HttpStatus.NOT_FOUND, "KYC_NOT_FOUND", "KYC 정보를 찾을 수 없습니다."),
    AI_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_REVIEW_NOT_FOUND", "AI 심사 정보를 찾을 수 없습니다."),
    AI_REVIEW_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_REVIEW_POLICY_NOT_FOUND", "AI 심사 정책을 찾을 수 없습니다."),
    INVALID_AI_REVIEW_POLICY(HttpStatus.BAD_REQUEST, "INVALID_AI_REVIEW_POLICY", "유효하지 않은 AI 심사 정책입니다."),
    INVALID_AI_REVIEW_RETRY_STATUS(HttpStatus.BAD_REQUEST, "INVALID_AI_REVIEW_RETRY_STATUS", "현재 상태에서는 AI 재심사를 요청할 수 없습니다."),
    // HTTP 403 - KYC 리소스 접근 권한 없음
    KYC_ACCESS_DENIED(HttpStatus.FORBIDDEN, "KYC_ACCESS_DENIED", "KYC 접근 권한이 없습니다."),
    // HTTP 409 - 이미 제출된 KYC 재제출 시도
    KYC_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "KYC_ALREADY_SUBMITTED", "이미 제출된 KYC입니다."),
    // HTTP 409 - 허용되지 않은 KYC 상태
    KYC_INVALID_STATUS(HttpStatus.CONFLICT, "KYC_INVALID_STATUS", "유효하지 않은 KYC 상태입니다."),
    // HTTP 404 - 문서 정보 조회 실패
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."),
    CREDENTIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "CREDENTIAL_NOT_FOUND", "Credential 정보를 찾을 수 없습니다."),
    INVALID_CREDENTIAL_ISSUE_STATUS(HttpStatus.CONFLICT, "INVALID_CREDENTIAL_ISSUE_STATUS", "현재 상태에서는 Credential 발급을 요청할 수 없습니다."),
    ISSUER_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "ISSUER_POLICY_NOT_FOUND", "Issuer 정책을 찾을 수 없습니다."),
    ISSUER_POLICY_DUPLICATED(HttpStatus.CONFLICT, "ISSUER_POLICY_DUPLICATED", "이미 존재하는 Issuer 정책입니다."),
    ISSUER_POLICY_CONFLICT(HttpStatus.CONFLICT, "ISSUER_POLICY_CONFLICT", "기존 Issuer 정책과 충돌합니다."),
    ISSUER_CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, "ISSUER_CONFIG_NOT_FOUND", "Issuer 설정을 찾을 수 없습니다."),
    INVALID_CODE_VALUE(HttpStatus.BAD_REQUEST, "INVALID_CODE_VALUE", "유효하지 않은 코드 값입니다."),
    MFA_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "MFA_TOKEN_INVALID", "MFA 토큰이 유효하지 않습니다."),
    CORE_REQUEST_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CORE_REQUEST_CREATE_FAILED", "Core 요청 생성에 실패했습니다."),
    // HTTP 403 - 문서 리소스 접근 권한 없음
    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "DOCUMENT_ACCESS_DENIED", "문서 접근 권한이 없습니다."),
    // HTTP 400 - 필수 문서 누락
    DOCUMENT_REQUIRED_MISSING(HttpStatus.BAD_REQUEST, "DOCUMENT_REQUIRED_MISSING", "필수 문서가 누락되었습니다."),
    // HTTP 404 - 공통 코드 조회 실패
    COMMON_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_CODE_NOT_FOUND", "공통 코드를 찾을 수 없습니다."),
    // HTTP 404 - 공통 코드 그룹 조회 실패
    COMMON_CODE_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_CODE_GROUP_NOT_FOUND", "공통 코드 그룹을 찾을 수 없습니다."),

    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND", "관리자를 찾을 수 없습니다."),
    ADMIN_ALREADY_EXISTS(HttpStatus.CONFLICT, "ADMIN_ALREADY_EXISTS", "이미 존재하는 관리자입니다."),
    ADMIN_INACTIVE(HttpStatus.FORBIDDEN, "ADMIN_INACTIVE", "비활성 관리자입니다."),
    ADMIN_LOCKED(HttpStatus.FORBIDDEN, "ADMIN_LOCKED", "잠금 처리된 관리자입니다."),
    ADMIN_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_ROLE_NOT_FOUND", "관리자 권한을 찾을 수 없습니다."),
    ADMIN_ROLE_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "ADMIN_ROLE_ALREADY_ASSIGNED", "이미 할당된 관리자 권한입니다."),
    ADMIN_ROLE_NOT_ASSIGNED(HttpStatus.NOT_FOUND, "ADMIN_ROLE_NOT_ASSIGNED", "할당된 관리자 권한을 찾을 수 없습니다."),
    MFA_NOT_FOUND(HttpStatus.NOT_FOUND, "MFA_NOT_FOUND", "MFA 정보를 찾을 수 없습니다."),
    MFA_EXPIRED(HttpStatus.UNAUTHORIZED, "MFA_EXPIRED", "MFA 인증이 만료되었습니다."),
    MFA_INVALID_CODE(HttpStatus.UNAUTHORIZED, "MFA_INVALID_CODE", "MFA 인증 코드가 올바르지 않습니다."),
    MFA_ALREADY_USED(HttpStatus.CONFLICT, "MFA_ALREADY_USED", "이미 사용된 MFA 인증입니다."),
    EMAIL_CONFIGURATION_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_CONFIGURATION_INVALID", "이메일 설정이 올바르지 않습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_SEND_FAILED", "이메일 발송에 실패했습니다."),
    PASSWORD_RESET_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "PASSWORD_RESET_TOKEN_NOT_FOUND", "비밀번호 재설정 토큰을 찾을 수 없습니다."),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "PASSWORD_RESET_TOKEN_EXPIRED", "비밀번호 재설정 토큰이 만료되었습니다."),
    AUDIT_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "AUDIT_LOG_NOT_FOUND", "감사 로그를 찾을 수 없습니다."),
    SUPPLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SUPPLEMENT_NOT_FOUND", "보완 요청을 찾을 수 없습니다."),
    DOCUMENT_REQUIREMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_REQUIREMENT_NOT_FOUND", "문서 제출 요건을 찾을 수 없습니다."),
    DOCUMENT_REQUIREMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "DOCUMENT_REQUIREMENT_ALREADY_EXISTS", "이미 존재하는 문서 제출 요건입니다."),
    COMMON_CODE_DISABLED(HttpStatus.CONFLICT, "COMMON_CODE_DISABLED", "비활성화된 공통 코드입니다."),
    COMMON_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "COMMON_CODE_ALREADY_EXISTS", "이미 존재하는 공통 코드입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(
            HttpStatus status, // HTTP 상태
            String code, // 코드 문자열
            String message // 기본 메시지
    ) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
