package com.kyvc.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// KYvC Backend API 공통 에러 코드
@Getter
public enum ErrorCode {

    // HTTP 400 - 잘못된 요청
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    // HTTP 401 - 인증 필요
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    // HTTP 403 - 권한 없음
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    // HTTP 404 - 리소스 없음
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    // HTTP 409 - 중복 리소스
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 존재하는 리소스입니다."),
    // HTTP 409 - 잘못된 상태
    INVALID_STATUS(HttpStatus.CONFLICT, "INVALID_STATUS", "허용되지 않는 상태입니다."),
    // HTTP 500 - 파일 업로드 실패
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_UPLOAD_FAILED", "파일 업로드에 실패했습니다."),
    // HTTP 500 - 이메일 발송 실패
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_SEND_FAILED", "이메일 발송에 실패했습니다."),
    // HTTP 500 - 이메일 설정 오류
    EMAIL_CONFIGURATION_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_CONFIGURATION_INVALID", "이메일 설정이 올바르지 않습니다."),
    // HTTP 500 - 서버 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),

    // HTTP 401 - 로그인 실패
    AUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_LOGIN_FAILED", "로그인에 실패했습니다."),
    // HTTP 401 - 토큰 만료
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_EXPIRED", "인증 토큰이 만료되었습니다."),
    // HTTP 401 - 토큰 오류
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_INVALID", "인증 토큰이 유효하지 않습니다."),
    // HTTP 401 - Refresh Token 조회 실패
    AUTH_REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_NOT_FOUND", "리프레시 토큰을 찾을 수 없습니다."),
    // HTTP 401 - Refresh Token 폐기됨
    AUTH_REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_REVOKED", "리프레시 토큰이 폐기되었습니다."),
    // HTTP 401 - 토큰 유형 오류
    AUTH_INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_TOKEN_TYPE", "토큰 유형이 올바르지 않습니다."),
    // HTTP 404 - 사용자 없음
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    // HTTP 409 - 사용자 중복
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "이미 존재하는 사용자입니다."),
    // HTTP 403 - 비활성 사용자
    USER_INACTIVE(HttpStatus.FORBIDDEN, "USER_INACTIVE", "비활성 사용자입니다."),
    // HTTP 404 - 법인 없음
    CORPORATE_NOT_FOUND(HttpStatus.NOT_FOUND, "CORPORATE_NOT_FOUND", "기업 정보를 찾을 수 없습니다."),
    // HTTP 403 - 법인 접근 거부
    CORPORATE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CORPORATE_ACCESS_DENIED", "기업 접근 권한이 없습니다."),
    // HTTP 404 - KYC 없음
    KYC_NOT_FOUND(HttpStatus.NOT_FOUND, "KYC_NOT_FOUND", "KYC 정보를 찾을 수 없습니다."),
    // HTTP 403 - KYC 접근 거부
    KYC_ACCESS_DENIED(HttpStatus.FORBIDDEN, "KYC_ACCESS_DENIED", "KYC 접근 권한이 없습니다."),
    // HTTP 409 - KYC 이미 제출됨
    KYC_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "KYC_ALREADY_SUBMITTED", "이미 제출된 KYC입니다."),
    // HTTP 409 - KYC 상태 오류
    KYC_INVALID_STATUS(HttpStatus.CONFLICT, "KYC_INVALID_STATUS", "유효하지 않은 KYC 상태입니다."),
    // HTTP 409 - 진행 중인 KYC 존재
    KYC_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "KYC_ALREADY_IN_PROGRESS", "진행 중인 KYC 요청이 이미 존재합니다."),
    // HTTP 400 - KYC 법인 정보 필요
    KYC_CORPORATE_REQUIRED(HttpStatus.BAD_REQUEST, "KYC_CORPORATE_REQUIRED", "KYC 요청에 필요한 법인 정보가 없습니다."),
    // HTTP 404 - 문서 없음
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."),
    // HTTP 403 - 문서 접근 거부
    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "DOCUMENT_ACCESS_DENIED", "문서 접근 권한이 없습니다."),
    // HTTP 400 - 필수 문서 누락
    DOCUMENT_REQUIRED_MISSING(HttpStatus.BAD_REQUEST, "DOCUMENT_REQUIRED_MISSING", "필수 문서가 누락되었습니다."),
    // HTTP 400 - 문서 확장자 오류
    DOCUMENT_INVALID_EXTENSION(HttpStatus.BAD_REQUEST, "DOCUMENT_INVALID_EXTENSION", "허용되지 않는 파일 확장자입니다."),
    // HTTP 400 - 문서 크기 초과
    DOCUMENT_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "DOCUMENT_SIZE_EXCEEDED", "파일 크기 제한을 초과했습니다."),
    // HTTP 500 - 문서 저장 경로 오류
    DOCUMENT_STORAGE_PATH_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "DOCUMENT_STORAGE_PATH_INVALID", "문서 저장 경로가 올바르지 않습니다."),

    // HTTP 404 - 보완 요청 없음
    SUPPLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SUPPLEMENT_NOT_FOUND", "보완 요청을 찾을 수 없습니다."),
    // HTTP 403 - 보완 요청 접근 거부
    SUPPLEMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SUPPLEMENT_ACCESS_DENIED", "보완 요청 접근 권한이 없습니다."),
    // HTTP 409 - 보완 요청 이미 제출됨
    SUPPLEMENT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "SUPPLEMENT_ALREADY_SUBMITTED", "이미 제출된 보완 요청입니다."),
    // HTTP 409 - 보완 요청 상태 오류
    SUPPLEMENT_INVALID_STATUS(HttpStatus.CONFLICT, "SUPPLEMENT_INVALID_STATUS", "보완 요청 상태가 올바르지 않습니다."),
    // HTTP 400 - 필수 보완 문서 누락
    SUPPLEMENT_REQUIRED_DOCUMENT_MISSING(HttpStatus.BAD_REQUEST, "SUPPLEMENT_REQUIRED_DOCUMENT_MISSING", "필수 보완 문서가 누락되었습니다."),
    // HTTP 400 - 허용되지 않는 보완 문서 유형
    SUPPLEMENT_DOCUMENT_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SUPPLEMENT_DOCUMENT_TYPE_NOT_ALLOWED", "보완 대상이 아닌 문서 유형입니다."),

    // HTTP 404 - KYC 심사 결과 없음
    KYC_REVIEW_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "KYC_REVIEW_RESULT_NOT_FOUND", "KYC 심사 결과를 찾을 수 없습니다."),
    // HTTP 409 - KYC 완료 화면 조회 불가
    KYC_COMPLETION_NOT_AVAILABLE(HttpStatus.CONFLICT, "KYC_COMPLETION_NOT_AVAILABLE", "KYC 완료 화면을 조회할 수 없는 상태입니다."),
    // HTTP 409 - Credential 안내 조회 불가
    CREDENTIAL_GUIDE_NOT_AVAILABLE(HttpStatus.CONFLICT, "CREDENTIAL_GUIDE_NOT_AVAILABLE", "Credential 발급 안내를 조회할 수 없습니다."),
    // HTTP 404 - Credential 없음
    CREDENTIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "CREDENTIAL_NOT_FOUND", "Credential을 찾을 수 없습니다."),
    // HTTP 403 - Credential 접근 거부
    CREDENTIAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CREDENTIAL_ACCESS_DENIED", "Credential 접근 권한이 없습니다."),
    // HTTP 409 - Credential 상태 오류
    CREDENTIAL_NOT_VALID(HttpStatus.CONFLICT, "CREDENTIAL_NOT_VALID", "유효한 Credential 상태가 아닙니다."),
    // HTTP 404 - Credential Offer 없음
    CREDENTIAL_OFFER_NOT_FOUND(HttpStatus.NOT_FOUND, "CREDENTIAL_OFFER_NOT_FOUND", "Credential Offer를 찾을 수 없습니다."),
    // HTTP 410 - Credential Offer 만료
    CREDENTIAL_OFFER_EXPIRED(HttpStatus.GONE, "CREDENTIAL_OFFER_EXPIRED", "Credential Offer가 만료되었습니다."),
    // HTTP 400 - Credential Offer 토큰 불일치
    CREDENTIAL_OFFER_INVALID_TOKEN(HttpStatus.BAD_REQUEST, "CREDENTIAL_OFFER_INVALID_TOKEN", "Credential Offer 토큰이 올바르지 않습니다."),
    // HTTP 409 - Credential Offer 이미 사용됨
    CREDENTIAL_OFFER_ALREADY_USED(HttpStatus.CONFLICT, "CREDENTIAL_OFFER_ALREADY_USED", "이미 사용된 Credential Offer입니다."),
    // HTTP 404 - Wallet Credential 없음
    WALLET_CREDENTIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "WALLET_CREDENTIAL_NOT_FOUND", "Wallet 저장 Credential을 찾을 수 없습니다."),
    // HTTP 409 - Wallet Credential 이미 저장됨
    WALLET_CREDENTIAL_ALREADY_SAVED(HttpStatus.CONFLICT, "WALLET_CREDENTIAL_ALREADY_SAVED", "이미 Wallet에 저장된 Credential입니다."),
    // HTTP 404 - 알림 없음
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
    // HTTP 403 - 알림 접근 거부
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "NOTIFICATION_ACCESS_DENIED", "알림 접근 권한이 없습니다."),
    // HTTP 500 - 감사 로그 저장 실패
    AUDIT_LOG_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUDIT_LOG_SAVE_FAILED", "감사 로그 저장에 실패했습니다."),
    // HTTP 404 - 모바일 기기 없음
    MOBILE_DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "MOBILE_DEVICE_NOT_FOUND", "모바일 기기를 찾을 수 없습니다."),
    // HTTP 403 - 모바일 기기 접근 거부
    MOBILE_DEVICE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MOBILE_DEVICE_ACCESS_DENIED", "모바일 기기 접근 권한이 없습니다."),
    // HTTP 409 - 모바일 기기 중복 등록
    MOBILE_DEVICE_ALREADY_REGISTERED(HttpStatus.CONFLICT, "MOBILE_DEVICE_ALREADY_REGISTERED", "이미 등록된 모바일 기기입니다."),
    // HTTP 400 - 모바일 기기 오류
    MOBILE_INVALID_DEVICE(HttpStatus.BAD_REQUEST, "MOBILE_INVALID_DEVICE", "유효하지 않은 모바일 기기 정보입니다."),
    // HTTP 404 - Wallet 기기 등록 정보 없음
    WALLET_DEVICE_NOT_REGISTERED(HttpStatus.NOT_FOUND, "WALLET_DEVICE_NOT_REGISTERED", "Wallet 기기 등록 정보를 찾을 수 없습니다."),
    // HTTP 403 - Wallet 기기 비활성
    WALLET_DEVICE_INACTIVE(HttpStatus.FORBIDDEN, "WALLET_DEVICE_INACTIVE", "Wallet 기기가 활성 상태가 아닙니다."),
    // HTTP 404 - 모바일 보안 설정 없음
    MOBILE_SECURITY_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "MOBILE_SECURITY_SETTING_NOT_FOUND", "모바일 보안 설정을 찾을 수 없습니다."),
    // HTTP 404 - Issuer 정책 없음
    ISSUER_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "ISSUER_POLICY_NOT_FOUND", "Issuer 정책을 찾을 수 없습니다."),
    // HTTP 403 - Issuer 정책 접근 거부
    ISSUER_POLICY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ISSUER_POLICY_ACCESS_DENIED", "Issuer 정책 접근 권한이 없습니다."),

    // HTTP 404 - MFA 요청 없음
    MFA_CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "MFA_CHALLENGE_NOT_FOUND", "MFA 인증 요청을 찾을 수 없습니다."),
    // HTTP 400 - MFA 코드 불일치
    MFA_CODE_INVALID(HttpStatus.BAD_REQUEST, "MFA_CODE_INVALID", "MFA 인증 코드가 올바르지 않습니다."),
    // HTTP 410 - MFA 코드 만료
    MFA_CODE_EXPIRED(HttpStatus.GONE, "MFA_CODE_EXPIRED", "MFA 인증 코드가 만료되었습니다."),
    // HTTP 429 - MFA 시도 횟수 초과
    MFA_CODE_ATTEMPT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "MFA_CODE_ATTEMPT_EXCEEDED", "MFA 인증 시도 횟수를 초과했습니다."),

    // HTTP 400 - 비밀번호 재설정 토큰 오류
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_INVALID", "비밀번호 재설정 토큰이 올바르지 않습니다."),
    // HTTP 410 - 비밀번호 재설정 토큰 만료
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.GONE, "PASSWORD_RESET_TOKEN_EXPIRED", "비밀번호 재설정 토큰이 만료되었습니다."),

    // HTTP 404 - 공통 코드 없음
    COMMON_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_CODE_NOT_FOUND", "공통 코드를 찾을 수 없습니다."),
    // HTTP 404 - 공통 코드 그룹 없음
    COMMON_CODE_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_CODE_GROUP_NOT_FOUND", "공통 코드 그룹을 찾을 수 없습니다."),
    // HTTP 409 - 문서 미리보기 사용 불가
    DOCUMENT_PREVIEW_NOT_AVAILABLE(HttpStatus.CONFLICT, "DOCUMENT_PREVIEW_NOT_AVAILABLE", "문서 미리보기를 사용할 수 없습니다."),
    // HTTP 500 - 내부 알림 저장 실패
    INTERNAL_NOTIFICATION_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_NOTIFICATION_SAVE_FAILED", "내부 알림 저장에 실패했습니다."),
    // HTTP 500 - 내부 감사 로그 저장 실패
    INTERNAL_AUDIT_LOG_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_AUDIT_LOG_SAVE_FAILED", "내부 감사 로그 저장에 실패했습니다."),

    // HTTP 400 - QR Payload 파싱 오류
    QR_PAYLOAD_INVALID(HttpStatus.BAD_REQUEST, "QR_PAYLOAD_INVALID", "QR Payload가 올바르지 않습니다."),
    // HTTP 400 - 지원하지 않는 QR 유형
    QR_TYPE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "QR_TYPE_NOT_SUPPORTED", "지원되지 않는 QR 유형입니다."),
    // HTTP 404 - VP 요청 없음
    VP_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "VP_REQUEST_NOT_FOUND", "VP 요청을 찾을 수 없습니다."),
    // HTTP 410 - VP 요청 만료
    VP_REQUEST_EXPIRED(HttpStatus.GONE, "VP_REQUEST_EXPIRED", "VP 요청이 만료되었습니다."),
    // HTTP 409 - VP 요청 상태 오류
    VP_REQUEST_INVALID_STATUS(HttpStatus.CONFLICT, "VP_REQUEST_INVALID_STATUS", "VP 요청 상태가 제출 가능한 상태가 아닙니다."),
    // HTTP 409 - VP 제출 불가 Credential
    VP_CREDENTIAL_NOT_ELIGIBLE(HttpStatus.CONFLICT, "VP_CREDENTIAL_NOT_ELIGIBLE", "VP 제출 가능한 Credential이 아닙니다."),
    // HTTP 404 - VP 제출 결과 없음
    VP_PRESENTATION_NOT_FOUND(HttpStatus.NOT_FOUND, "VP_PRESENTATION_NOT_FOUND", "VP 제출 결과를 찾을 수 없습니다."),
    // HTTP 409 - VP Replay 의심
    VP_PRESENTATION_REPLAY_SUSPECTED(HttpStatus.CONFLICT, "VP_PRESENTATION_REPLAY_SUSPECTED", "VP 재사용 제출이 의심됩니다."),
    // HTTP 400 - VP nonce 불일치
    VP_NONCE_INVALID(HttpStatus.BAD_REQUEST, "VP_NONCE_INVALID", "VP nonce가 올바르지 않습니다."),
    // HTTP 400 - VP challenge 불일치
    VP_CHALLENGE_INVALID(HttpStatus.BAD_REQUEST, "VP_CHALLENGE_INVALID", "VP challenge가 올바르지 않습니다."),
    // HTTP 400 - VP JWT 누락
    VP_JWT_REQUIRED(HttpStatus.BAD_REQUEST, "VP_JWT_REQUIRED", "VP JWT가 필요합니다.");

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
