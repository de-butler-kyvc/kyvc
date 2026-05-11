package com.kyvc.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// KYvC Backend API 오류 코드
@Getter
public enum ErrorCode {

    // HTTP 400 - 요청 오류
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청이 올바르지 않습니다."),
    // HTTP 401 - 인증 필요
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    // HTTP 403 - 접근 거부
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    // HTTP 404 - 리소스 없음
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    // HTTP 409 - 중복 리소스
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 존재하는 리소스입니다."),
    // HTTP 409 - 상태 오류
    INVALID_STATUS(HttpStatus.CONFLICT, "INVALID_STATUS", "처리할 수 없는 상태입니다."),
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
    // HTTP 401 - 인증 토큰 만료
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_EXPIRED", "인증 토큰이 만료되었습니다."),
    // HTTP 401 - 인증 토큰 오류
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_INVALID", "인증 토큰이 올바르지 않습니다."),
    // HTTP 401 - Refresh Token 없음
    AUTH_REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_NOT_FOUND", "Refresh Token을 찾을 수 없습니다."),
    // HTTP 401 - Refresh Token 폐기
    AUTH_REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_REVOKED", "Refresh Token이 폐기되었습니다."),
    // HTTP 401 - 토큰 유형 오류
    AUTH_INVALID_TOKEN_TYPE(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_TOKEN_TYPE", "토큰 유형이 올바르지 않습니다."),
    // HTTP 404 - 사용자 없음
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    // HTTP 409 - 사용자 중복
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "이미 존재하는 사용자입니다."),
    // HTTP 403 - 사용자 비활성
    USER_INACTIVE(HttpStatus.FORBIDDEN, "USER_INACTIVE", "비활성 사용자입니다."),
    // HTTP 404 - 역할 없음
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "역할 정보를 찾을 수 없습니다."),
    // HTTP 403 - 사용자 역할 없음
    USER_ROLE_NOT_FOUND(HttpStatus.FORBIDDEN, "USER_ROLE_NOT_FOUND", "사용자 역할 정보를 찾을 수 없습니다."),
    // HTTP 403 - 역할 선택 오류
    INVALID_ROLE_SELECTION(HttpStatus.FORBIDDEN, "INVALID_ROLE_SELECTION", "선택할 수 없는 역할입니다."),
    // HTTP 400 - 현재 비밀번호 불일치
    CURRENT_PASSWORD_NOT_MATCHED(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_NOT_MATCHED", "현재 비밀번호가 일치하지 않습니다."),
    // HTTP 400 - 비밀번호 확인 불일치
    PASSWORD_CONFIRM_NOT_MATCHED(HttpStatus.BAD_REQUEST, "PASSWORD_CONFIRM_NOT_MATCHED", "새 비밀번호 확인값이 일치하지 않습니다."),
    // HTTP 400 - MFA 검증 필요
    MFA_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "MFA_VERIFICATION_REQUIRED", "MFA 검증이 필요합니다."),
    // HTTP 404 - 법인 없음
    CORPORATE_NOT_FOUND(HttpStatus.NOT_FOUND, "CORPORATE_NOT_FOUND", "법인 정보를 찾을 수 없습니다."),
    // HTTP 403 - 법인 접근 거부
    CORPORATE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CORPORATE_ACCESS_DENIED", "법인 정보 접근 권한이 없습니다."),
    // HTTP 404 - 대리인 없음
    AGENT_NOT_FOUND(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "대리인 정보를 찾을 수 없습니다."),
    // HTTP 404 - KYC 없음
    KYC_NOT_FOUND(HttpStatus.NOT_FOUND, "KYC_NOT_FOUND", "KYC 신청을 찾을 수 없습니다."),
    // HTTP 403 - KYC 접근 거부
    KYC_ACCESS_DENIED(HttpStatus.FORBIDDEN, "KYC_ACCESS_DENIED", "KYC 신청 접근 권한이 없습니다."),
    // HTTP 409 - KYC 제출 중복
    KYC_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "KYC_ALREADY_SUBMITTED", "이미 제출된 KYC 신청입니다."),
    // HTTP 409 - KYC 상태 오류
    KYC_INVALID_STATUS(HttpStatus.CONFLICT, "KYC_INVALID_STATUS", "처리할 수 없는 KYC 상태입니다."),
    // HTTP 409 - 진행 중 KYC 존재
    KYC_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "KYC_ALREADY_IN_PROGRESS", "진행 중인 KYC 신청이 이미 존재합니다."),
    // HTTP 400 - KYC 법인 필요
    KYC_CORPORATE_REQUIRED(HttpStatus.BAD_REQUEST, "KYC_CORPORATE_REQUIRED", "KYC 신청에 필요한 법인 정보가 없습니다."),
    // HTTP 404 - 문서 없음
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."),
    // HTTP 403 - 문서 접근 거부
    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "DOCUMENT_ACCESS_DENIED", "문서 접근 권한이 없습니다."),
    // HTTP 400 - 필수 문서 누락
    DOCUMENT_REQUIRED_MISSING(HttpStatus.BAD_REQUEST, "DOCUMENT_REQUIRED_MISSING", "필수 문서가 누락되었습니다."),
    // HTTP 400 - 문서 파일 누락
    DOCUMENT_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "DOCUMENT_FILE_REQUIRED", "업로드할 문서 파일이 필요합니다."),
    // HTTP 400 - 문서 확장자 오류
    DOCUMENT_INVALID_EXTENSION(HttpStatus.BAD_REQUEST, "DOCUMENT_INVALID_EXTENSION", "허용되지 않는 문서 확장자입니다."),
    // HTTP 400 - 문서 MIME 유형 오류
    DOCUMENT_MIME_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "DOCUMENT_MIME_TYPE_NOT_ALLOWED", "허용되지 않는 문서 MIME 유형입니다."),
    // HTTP 400 - 문서 크기 초과
    DOCUMENT_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "DOCUMENT_SIZE_EXCEEDED", "문서 크기가 허용 범위를 초과했습니다."),
    // HTTP 500 - 문서 저장 실패
    DOCUMENT_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "DOCUMENT_SAVE_FAILED", "문서 저장에 실패했습니다."),
    // HTTP 404 - 문서 파일 없음
    DOCUMENT_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_FILE_NOT_FOUND", "문서 파일을 찾을 수 없습니다."),
    // HTTP 500 - 문서 저장 경로 오류
    DOCUMENT_STORAGE_PATH_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "DOCUMENT_STORAGE_PATH_INVALID", "문서 저장 경로가 올바르지 않습니다."),
    // HTTP 409 - 문서 삭제 요청 중복
    DOCUMENT_DELETE_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "DOCUMENT_DELETE_REQUEST_ALREADY_EXISTS", "진행 중인 문서 삭제 요청이 이미 존재합니다."),
    // HTTP 404 - 문서 삭제 요청 없음
    DOCUMENT_DELETE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_DELETE_REQUEST_NOT_FOUND", "문서 삭제 요청을 찾을 수 없습니다."),

    // HTTP 403 - 금융사 직원 권한 필요
    FINANCE_STAFF_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "FINANCE_STAFF_ROLE_REQUIRED", "금융사 직원 권한이 필요합니다."),
    // HTTP 404 - 금융사 컨텍스트 없음
    FINANCE_CONTEXT_NOT_FOUND(HttpStatus.NOT_FOUND, "FINANCE_CONTEXT_NOT_FOUND", "금융사 컨텍스트를 찾을 수 없습니다."),
    // HTTP 404 - 금융사 고객 연결 없음
    FINANCE_CUSTOMER_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "FINANCE_CUSTOMER_LINK_NOT_FOUND", "금융사 고객 연결 정보를 찾을 수 없습니다."),
    // HTTP 409 - 금융사 고객 연결 중복
    FINANCE_CUSTOMER_LINK_ALREADY_EXISTS(HttpStatus.CONFLICT, "FINANCE_CUSTOMER_LINK_ALREADY_EXISTS", "이미 연결된 금융사 고객번호입니다."),
    // HTTP 404 - 금융사 KYC 없음
    FINANCE_KYC_NOT_FOUND(HttpStatus.NOT_FOUND, "FINANCE_KYC_NOT_FOUND", "금융사 KYC 신청을 찾을 수 없습니다."),
    // HTTP 403 - 금융사 KYC 접근 거부
    FINANCE_KYC_ACCESS_DENIED(HttpStatus.FORBIDDEN, "FINANCE_KYC_ACCESS_DENIED", "금융사 KYC 접근 권한이 없습니다."),
    // HTTP 409 - 금융사 KYC 상태 오류
    INVALID_FINANCE_KYC_STATUS(HttpStatus.CONFLICT, "INVALID_FINANCE_KYC_STATUS", "처리할 수 없는 금융사 KYC 상태입니다."),
    // HTTP 404 - 금융사 KYC 문서 없음
    FINANCE_KYC_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "FINANCE_KYC_DOCUMENT_NOT_FOUND", "금융사 KYC 문서를 찾을 수 없습니다."),
    // HTTP 403 - 금융사 KYC 문서 접근 거부
    FINANCE_KYC_DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "FINANCE_KYC_DOCUMENT_ACCESS_DENIED", "금융사 KYC 문서 접근 권한이 없습니다."),
    // HTTP 409 - 금융사 KYC 문서 업로드 불가
    FINANCE_KYC_DOCUMENT_UPLOAD_NOT_ALLOWED(HttpStatus.CONFLICT, "FINANCE_KYC_DOCUMENT_UPLOAD_NOT_ALLOWED", "현재 상태에서는 금융사 KYC 문서를 업로드할 수 없습니다."),
    // HTTP 409 - 금융사 KYC 제출 불가
    FINANCE_KYC_SUBMIT_NOT_ALLOWED(HttpStatus.CONFLICT, "FINANCE_KYC_SUBMIT_NOT_ALLOWED", "현재 상태에서는 금융사 KYC를 제출할 수 없습니다."),
    // HTTP 400 - 금융사 KYC 필수 정보 누락
    FINANCE_KYC_REQUIRED_INFO_MISSING(HttpStatus.BAD_REQUEST, "FINANCE_KYC_REQUIRED_INFO_MISSING", "금융사 KYC 필수 정보가 누락되었습니다."),
    // HTTP 400 - 금융사 KYC 필수 문서 누락
    FINANCE_KYC_REQUIRED_DOCUMENT_MISSING(HttpStatus.BAD_REQUEST, "FINANCE_KYC_REQUIRED_DOCUMENT_MISSING", "금융사 KYC 필수 문서가 누락되었습니다."),
    // HTTP 502 - 금융사 KYC AI 심사 실패
    FINANCE_KYC_AI_REVIEW_FAILED(HttpStatus.BAD_GATEWAY, "FINANCE_KYC_AI_REVIEW_FAILED", "금융사 KYC AI 심사에 실패했습니다."),
    // HTTP 404 - 금융사 KYC 결과 없음
    FINANCE_KYC_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "FINANCE_KYC_RESULT_NOT_FOUND", "금융사 KYC 심사 결과를 찾을 수 없습니다."),
    // HTTP 409 - 금융사 KYC QR 발급 불가
    FINANCE_KYC_QR_ISSUE_NOT_ALLOWED(HttpStatus.CONFLICT, "FINANCE_KYC_QR_ISSUE_NOT_ALLOWED", "현재 상태에서는 금융사 KYC QR을 발급할 수 없습니다."),
    // HTTP 404 - 금융사 KYC QR 없음
    FINANCE_KYC_QR_NOT_FOUND(HttpStatus.NOT_FOUND, "FINANCE_KYC_QR_NOT_FOUND", "금융사 KYC QR 정보를 찾을 수 없습니다."),
    // HTTP 409 - Credential 발급 진행 중
    CREDENTIAL_ALREADY_ISSUING(HttpStatus.CONFLICT, "CREDENTIAL_ALREADY_ISSUING", "Credential 발급이 이미 진행 중입니다."),
    // HTTP 409 - Credential 발급 완료
    CREDENTIAL_ALREADY_VALID(HttpStatus.CONFLICT, "CREDENTIAL_ALREADY_VALID", "유효한 Credential이 이미 존재합니다."),
    // HTTP 502 - Core AI 심사 실패
    CORE_AI_REVIEW_FAILED(HttpStatus.BAD_GATEWAY, "CORE_AI_REVIEW_FAILED", "Core AI 심사 호출에 실패했습니다."),
    // HTTP 502 - Core VC 발급 실패
    CORE_VC_ISSUANCE_FAILED(HttpStatus.BAD_GATEWAY, "CORE_VC_ISSUANCE_FAILED", "Core VC 발급 호출에 실패했습니다."),

    // HTTP 404 - 보완 요청 없음
    SUPPLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SUPPLEMENT_NOT_FOUND", "보완 요청을 찾을 수 없습니다."),
    // HTTP 403 - 보완 요청 접근 거부
    SUPPLEMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SUPPLEMENT_ACCESS_DENIED", "보완 요청 접근 권한이 없습니다."),
    // HTTP 409 - 보완 제출 중복
    SUPPLEMENT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "SUPPLEMENT_ALREADY_SUBMITTED", "이미 제출된 보완 요청입니다."),
    // HTTP 409 - 보완 상태 오류
    SUPPLEMENT_INVALID_STATUS(HttpStatus.CONFLICT, "SUPPLEMENT_INVALID_STATUS", "보완 요청 상태가 올바르지 않습니다."),
    // HTTP 400 - 보완 필수 문서 누락
    SUPPLEMENT_REQUIRED_DOCUMENT_MISSING(HttpStatus.BAD_REQUEST, "SUPPLEMENT_REQUIRED_DOCUMENT_MISSING", "보완 필수 문서가 누락되었습니다."),
    // HTTP 400 - 보완 문서 유형 오류
    SUPPLEMENT_DOCUMENT_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SUPPLEMENT_DOCUMENT_TYPE_NOT_ALLOWED", "보완 요청에 허용되지 않는 문서 유형입니다."),

    // HTTP 404 - KYC 심사 결과 없음
    KYC_REVIEW_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "KYC_REVIEW_RESULT_NOT_FOUND", "KYC 심사 결과를 찾을 수 없습니다."),
    // HTTP 409 - KYC 완료 불가
    KYC_COMPLETION_NOT_AVAILABLE(HttpStatus.CONFLICT, "KYC_COMPLETION_NOT_AVAILABLE", "KYC 완료 정보를 확인할 수 없는 상태입니다."),
    // HTTP 409 - Credential 안내 불가
    CREDENTIAL_GUIDE_NOT_AVAILABLE(HttpStatus.CONFLICT, "CREDENTIAL_GUIDE_NOT_AVAILABLE", "Credential 발급 안내를 확인할 수 없는 상태입니다."),
    // HTTP 404 - Credential 없음
    CREDENTIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "CREDENTIAL_NOT_FOUND", "Credential을 찾을 수 없습니다."),
    // HTTP 403 - Credential 접근 거부
    CREDENTIAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CREDENTIAL_ACCESS_DENIED", "Credential 접근 권한이 없습니다."),
    // HTTP 409 - Credential 상태 오류
    CREDENTIAL_NOT_VALID(HttpStatus.CONFLICT, "CREDENTIAL_NOT_VALID", "유효한 Credential 상태가 아닙니다."),
    // HTTP 404 - Credential 요청 없음
    // HTTP 404 - Credential Offer 없음
    CREDENTIAL_OFFER_NOT_FOUND(HttpStatus.NOT_FOUND, "CREDENTIAL_OFFER_NOT_FOUND", "Credential Offer를 찾을 수 없습니다."),
    // HTTP 410 - Credential Offer 만료
    CREDENTIAL_OFFER_EXPIRED(HttpStatus.GONE, "CREDENTIAL_OFFER_EXPIRED", "Credential Offer가 만료되었습니다."),
    // HTTP 400 - Credential Offer 토큰 오류
    CREDENTIAL_OFFER_INVALID_TOKEN(HttpStatus.BAD_REQUEST, "CREDENTIAL_OFFER_INVALID_TOKEN", "Credential Offer 토큰이 올바르지 않습니다."),
    // HTTP 409 - Credential Offer 사용 완료
    CREDENTIAL_OFFER_ALREADY_USED(HttpStatus.CONFLICT, "CREDENTIAL_OFFER_ALREADY_USED", "이미 사용된 Credential Offer입니다."),
    // HTTP 404 - Wallet Credential 없음
    WALLET_CREDENTIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "WALLET_CREDENTIAL_NOT_FOUND", "Wallet Credential을 찾을 수 없습니다."),
    // HTTP 409 - Wallet Credential 중복 저장
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
    MOBILE_INVALID_DEVICE(HttpStatus.BAD_REQUEST, "MOBILE_INVALID_DEVICE", "모바일 기기 정보가 올바르지 않습니다."),
    // HTTP 404 - Wallet 기기 미등록
    WALLET_DEVICE_NOT_REGISTERED(HttpStatus.NOT_FOUND, "WALLET_DEVICE_NOT_REGISTERED", "Wallet 기기 등록 정보를 찾을 수 없습니다."),
    // HTTP 403 - Wallet 기기 비활성
    WALLET_DEVICE_INACTIVE(HttpStatus.FORBIDDEN, "WALLET_DEVICE_INACTIVE", "Wallet 기기가 활성 상태가 아닙니다."),
    // HTTP 404 - 모바일 보안 설정 없음
    MOBILE_SECURITY_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "MOBILE_SECURITY_SETTING_NOT_FOUND", "모바일 보안 설정을 찾을 수 없습니다."),
    // HTTP 404 - Issuer 정책 없음
    ISSUER_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "ISSUER_POLICY_NOT_FOUND", "Issuer 정책을 찾을 수 없습니다."),
    // HTTP 403 - Issuer 정책 접근 거부
    ISSUER_POLICY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ISSUER_POLICY_ACCESS_DENIED", "Issuer 정책 접근 권한이 없습니다."),

    // HTTP 404 - MFA 인증 요청 없음
    MFA_CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "MFA_CHALLENGE_NOT_FOUND", "MFA 인증 요청을 찾을 수 없습니다."),
    // HTTP 400 - MFA 코드 오류
    MFA_CODE_INVALID(HttpStatus.BAD_REQUEST, "MFA_CODE_INVALID", "MFA 인증 코드가 올바르지 않습니다."),
    // HTTP 410 - MFA 코드 만료
    MFA_CODE_EXPIRED(HttpStatus.GONE, "MFA_CODE_EXPIRED", "MFA 인증 코드가 만료되었습니다."),
    // HTTP 429 - MFA 시도 횟수 초과
    MFA_CODE_ATTEMPT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "MFA_CODE_ATTEMPT_EXCEEDED", "MFA 인증 시도 횟수를 초과했습니다."),

    // HTTP 400 - 비밀번호 재설정 토큰 오류
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_INVALID", "비밀번호 재설정 토큰이 올바르지 않습니다."),
    // HTTP 410 - 비밀번호 재설정 토큰 만료
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.GONE, "PASSWORD_RESET_TOKEN_EXPIRED", "비밀번호 재설정 토큰이 만료되었습니다."),

    // HTTP 404 - 공통코드 없음
    COMMON_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_CODE_NOT_FOUND", "공통코드를 찾을 수 없습니다."),
    // HTTP 404 - 공통코드 그룹 없음
    COMMON_CODE_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_CODE_GROUP_NOT_FOUND", "공통코드 그룹을 찾을 수 없습니다."),
    // HTTP 410 - 문서 미리보기 만료
    DOCUMENT_PREVIEW_EXPIRED(HttpStatus.GONE, "DOCUMENT_PREVIEW_EXPIRED", "문서 미리보기 URL이 만료되었습니다."),
    // HTTP 409 - 문서 미리보기 불가
    DOCUMENT_PREVIEW_NOT_AVAILABLE(HttpStatus.CONFLICT, "DOCUMENT_PREVIEW_NOT_AVAILABLE", "문서 미리보기를 사용할 수 없습니다."),
    // HTTP 500 - 내부 알림 저장 실패
    INTERNAL_NOTIFICATION_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_NOTIFICATION_SAVE_FAILED", "내부 알림 저장에 실패했습니다."),
    // HTTP 500 - 내부 감사 로그 저장 실패
    INTERNAL_AUDIT_LOG_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_AUDIT_LOG_SAVE_FAILED", "내부 감사 로그 저장에 실패했습니다."),

    // HTTP 400 - QR Payload 오류
    QR_PAYLOAD_INVALID(HttpStatus.BAD_REQUEST, "QR_PAYLOAD_INVALID", "QR Payload가 올바르지 않습니다."),
    // HTTP 400 - QR 유형 오류
    QR_TYPE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "QR_TYPE_NOT_SUPPORTED", "지원되지 않는 QR 유형입니다."),
    // HTTP 404 - VP 요청 없음
    VP_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "VP_REQUEST_NOT_FOUND", "VP 요청을 찾을 수 없습니다."),
    // HTTP 410 - VP 요청 만료
    VP_REQUEST_EXPIRED(HttpStatus.GONE, "VP_REQUEST_EXPIRED", "VP 요청이 만료되었습니다."),
    // HTTP 409 - VP 요청 상태 오류
    VP_REQUEST_INVALID_STATUS(HttpStatus.CONFLICT, "VP_REQUEST_INVALID_STATUS", "VP 요청 상태가 제출 가능한 상태가 아닙니다."),
    // HTTP 409 - VP 제출 Credential 오류
    VP_CREDENTIAL_NOT_ELIGIBLE(HttpStatus.CONFLICT, "VP_CREDENTIAL_NOT_ELIGIBLE", "VP 제출 가능한 Credential이 아닙니다."),
    // HTTP 404 - VP 제출 결과 없음
    VP_PRESENTATION_NOT_FOUND(HttpStatus.NOT_FOUND, "VP_PRESENTATION_NOT_FOUND", "VP 제출 결과를 찾을 수 없습니다."),
    // HTTP 409 - VP Replay 의심
    VP_PRESENTATION_REPLAY_SUSPECTED(HttpStatus.CONFLICT, "VP_PRESENTATION_REPLAY_SUSPECTED", "VP 재제출이 의심됩니다."),
    // HTTP 400 - VP nonce 오류
    VP_NONCE_INVALID(HttpStatus.BAD_REQUEST, "VP_NONCE_INVALID", "VP nonce가 올바르지 않습니다."),
    // HTTP 400 - VP challenge 오류
    VP_CHALLENGE_INVALID(HttpStatus.BAD_REQUEST, "VP_CHALLENGE_INVALID", "VP challenge가 올바르지 않습니다."),
    // HTTP 400 - VP JWT 필요
    VP_JWT_REQUIRED(HttpStatus.BAD_REQUEST, "VP_JWT_REQUIRED", "VP JWT가 필요합니다."),
    // HTTP 401 - Verifier API Key 필요
    VERIFIER_API_KEY_REQUIRED(HttpStatus.UNAUTHORIZED, "VERIFIER_API_KEY_REQUIRED", "Verifier API Key가 필요합니다."),
    // HTTP 401 - Verifier API Key 오류
    VERIFIER_API_KEY_INVALID(HttpStatus.UNAUTHORIZED, "VERIFIER_API_KEY_INVALID", "Verifier API Key가 올바르지 않습니다."),
    // HTTP 401 - Verifier API Key 만료
    VERIFIER_API_KEY_EXPIRED(HttpStatus.UNAUTHORIZED, "VERIFIER_API_KEY_EXPIRED", "Verifier API Key가 만료되었습니다."),
    // HTTP 403 - Verifier API Key 비활성
    VERIFIER_API_KEY_INACTIVE(HttpStatus.FORBIDDEN, "VERIFIER_API_KEY_INACTIVE", "Verifier API Key가 활성 상태가 아닙니다."),
    // HTTP 404 - Verifier 없음
    VERIFIER_NOT_FOUND(HttpStatus.NOT_FOUND, "VERIFIER_NOT_FOUND", "Verifier를 찾을 수 없습니다."),
    // HTTP 403 - Verifier 비활성
    VERIFIER_INACTIVE(HttpStatus.FORBIDDEN, "VERIFIER_INACTIVE", "Verifier가 활성 상태가 아닙니다."),
    // HTTP 403 - Verifier 접근 거부
    VERIFIER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "VERIFIER_ACCESS_DENIED", "Verifier 접근 권한이 없습니다."),
    // HTTP 404 - Verifier 테스트 검증 없음
    VERIFIER_TEST_NOT_FOUND(HttpStatus.NOT_FOUND, "VERIFIER_TEST_NOT_FOUND", "Verifier 테스트 검증 이력을 찾을 수 없습니다."),
    // HTTP 404 - Verifier 테스트 검증 결과 없음
    VERIFIER_TEST_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "VERIFIER_TEST_RESULT_NOT_FOUND", "Verifier 테스트 검증 결과를 찾을 수 없습니다."),
    // HTTP 400 - Verifier 사용량 기간 오류
    VERIFIER_USAGE_STATS_INVALID_RANGE(HttpStatus.BAD_REQUEST, "VERIFIER_USAGE_STATS_INVALID_RANGE", "Verifier 사용량 조회 기간이 올바르지 않습니다."),
    // HTTP 401 - 내부 API Key 필요
    INTERNAL_API_KEY_REQUIRED(HttpStatus.UNAUTHORIZED, "INTERNAL_API_KEY_REQUIRED", "내부 API Key가 필요합니다."),
    // HTTP 401 - 내부 API Key 오류
    INTERNAL_API_KEY_INVALID(HttpStatus.UNAUTHORIZED, "INTERNAL_API_KEY_INVALID", "내부 API Key가 올바르지 않습니다."),
    // HTTP 404 - 알림 템플릿 없음
    NOTIFICATION_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_TEMPLATE_NOT_FOUND", "알림 템플릿을 찾을 수 없습니다."),
    // HTTP 400 - 대량 알림 대상 오류
    NOTIFICATION_BULK_TARGET_INVALID(HttpStatus.BAD_REQUEST, "NOTIFICATION_BULK_TARGET_INVALID", "대량 알림 대상 유형이 올바르지 않습니다."),
    // HTTP 400 - 대량 알림 사용자 목록 없음
    NOTIFICATION_BULK_USER_EMPTY(HttpStatus.BAD_REQUEST, "NOTIFICATION_BULK_USER_EMPTY", "대량 알림 사용자 목록이 필요합니다."),
    // HTTP 400 - 결과 통지 URL 오류
    INVALID_NOTIFY_URL(HttpStatus.BAD_REQUEST, "INVALID_NOTIFY_URL", "결과 통지 URL이 올바르지 않습니다."),

    // HTTP 404 - DID 기관 없음
    DID_INSTITUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "DID_INSTITUTION_NOT_FOUND", "DID 기관 정보를 찾을 수 없습니다."),
    // HTTP 400 - DID 형식 오류
    INVALID_DID(HttpStatus.BAD_REQUEST, "INVALID_DID", "DID 형식이 올바르지 않습니다."),

    // HTTP 502 - Core API 호출 실패
    CORE_API_CALL_FAILED(HttpStatus.BAD_GATEWAY, "CORE_API_CALL_FAILED", "Core API 호출에 실패했습니다."),
    // HTTP 504 - Core API Timeout
    CORE_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "CORE_API_TIMEOUT", "Core API 호출 시간이 초과되었습니다."),
    // HTTP 502 - Core API 응답 오류
    CORE_API_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "CORE_API_RESPONSE_INVALID", "Core API 응답 형식이 올바르지 않습니다."),
    // HTTP 400 - Core 필수 데이터 누락
    CORE_REQUIRED_DATA_MISSING(HttpStatus.BAD_REQUEST, "CORE_REQUIRED_DATA_MISSING", "Core API 호출에 필요한 데이터가 부족합니다."),
    // HTTP 409 - Core 미지원 작업
    CORE_UNSUPPORTED_OPERATION(HttpStatus.CONFLICT, "CORE_UNSUPPORTED_OPERATION", "지원하지 않는 Core 작업입니다."),
    // HTTP 409 - Core 개발 Seed 비활성
    CORE_DEV_SEED_DISABLED(HttpStatus.CONFLICT, "CORE_DEV_SEED_DISABLED", "Core 개발 Seed 사용이 비활성화되어 있습니다."),
    // HTTP 409 - KYC 임시 승인 불가
    KYC_APPROVAL_NOT_ALLOWED(HttpStatus.CONFLICT, "KYC_APPROVAL_NOT_ALLOWED", "현재 상태에서는 KYC 승인을 처리할 수 없습니다."),
    // HTTP 409 - KYC 반려 후 임시 승인 불가
    KYC_ALREADY_REJECTED(HttpStatus.CONFLICT, "KYC_ALREADY_REJECTED", "이미 반려된 KYC 신청은 임시 승인할 수 없습니다."),
    // HTTP 409 - VC 발급 요청 불가
    CREDENTIAL_ISSUANCE_NOT_ALLOWED(HttpStatus.CONFLICT, "CREDENTIAL_ISSUANCE_NOT_ALLOWED", "현재 상태에서는 VC 발급을 요청할 수 없습니다."),
    // HTTP 409 - VC 발급 완료
    CREDENTIAL_ISSUANCE_ALREADY_COMPLETED(HttpStatus.CONFLICT, "CREDENTIAL_ISSUANCE_ALREADY_COMPLETED", "이미 VC 발급이 완료되었습니다."),
    // HTTP 409 - Credential 요청 중복
    CREDENTIAL_REQUEST_DUPLICATED(HttpStatus.CONFLICT, "CREDENTIAL_REQUEST_DUPLICATED", "진행 중인 Credential 요청이 이미 존재합니다."),
    // HTTP 409 - Credential 재발급 불가
    CREDENTIAL_REISSUE_NOT_ALLOWED(HttpStatus.CONFLICT, "CREDENTIAL_REISSUE_NOT_ALLOWED", "현재 상태에서는 VC 재발급을 요청할 수 없습니다."),
    // HTTP 409 - Credential 폐기 불가
    CREDENTIAL_REVOKE_NOT_ALLOWED(HttpStatus.CONFLICT, "CREDENTIAL_REVOKE_NOT_ALLOWED", "현재 상태에서는 VC 폐기를 요청할 수 없습니다."),
    // HTTP 404 - Credential 요청 이력 없음
    CREDENTIAL_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "CREDENTIAL_REQUEST_NOT_FOUND", "Credential 요청 이력을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(
            HttpStatus status, // HTTP 상태
            String code, // 오류 코드
            String message // 응답 메시지
    ) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
