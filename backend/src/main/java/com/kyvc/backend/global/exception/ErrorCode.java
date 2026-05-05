package com.kyvc.backend.global.exception;

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
    // HTTP 403 - KYC 리소스 접근 권한 없음
    KYC_ACCESS_DENIED(HttpStatus.FORBIDDEN, "KYC_ACCESS_DENIED", "KYC 접근 권한이 없습니다."),
    // HTTP 409 - 이미 제출된 KYC 재제출 시도
    KYC_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "KYC_ALREADY_SUBMITTED", "이미 제출된 KYC입니다."),
    // HTTP 409 - 허용되지 않은 KYC 상태
    KYC_INVALID_STATUS(HttpStatus.CONFLICT, "KYC_INVALID_STATUS", "유효하지 않은 KYC 상태입니다."),
    // HTTP 409 - 진행 중인 KYC 요청 존재
    KYC_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "KYC_ALREADY_IN_PROGRESS", "진행 중인 KYC 요청이 이미 존재합니다."),
    // HTTP 400 - KYC 요청 법인정보 누락
    KYC_CORPORATE_REQUIRED(HttpStatus.BAD_REQUEST, "KYC_CORPORATE_REQUIRED", "KYC 요청을 위한 법인정보가 필요합니다."),
    // HTTP 404 - 문서 정보 조회 실패
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다."),
    // HTTP 403 - 문서 리소스 접근 권한 없음
    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "DOCUMENT_ACCESS_DENIED", "문서 접근 권한이 없습니다."),
    // HTTP 400 - 필수 문서 누락
    DOCUMENT_REQUIRED_MISSING(HttpStatus.BAD_REQUEST, "DOCUMENT_REQUIRED_MISSING", "필수 문서가 누락되었습니다."),
    // HTTP 400 - 허용되지 않은 문서 확장자
    DOCUMENT_INVALID_EXTENSION(HttpStatus.BAD_REQUEST, "DOCUMENT_INVALID_EXTENSION", "허용되지 않은 파일 확장자입니다."),
    // HTTP 400 - 문서 파일 크기 제한 초과
    DOCUMENT_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "DOCUMENT_SIZE_EXCEEDED", "파일 크기 제한을 초과했습니다."),
    // HTTP 500 - 문서 저장 경로 오류
    DOCUMENT_STORAGE_PATH_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "DOCUMENT_STORAGE_PATH_INVALID", "문서 저장 경로가 올바르지 않습니다."),

    // HTTP 404 - 보완요청 조회 실패
    SUPPLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SUPPLEMENT_NOT_FOUND", "보완요청을 찾을 수 없습니다."),
    // HTTP 403 - 보완요청 접근 권한 없음
    SUPPLEMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SUPPLEMENT_ACCESS_DENIED", "보완요청 접근 권한이 없습니다."),
    // HTTP 409 - 이미 제출된 보완요청
    SUPPLEMENT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "SUPPLEMENT_ALREADY_SUBMITTED", "이미 제출된 보완요청입니다."),
    // HTTP 409 - 허용되지 않은 보완요청 상태
    SUPPLEMENT_INVALID_STATUS(HttpStatus.CONFLICT, "SUPPLEMENT_INVALID_STATUS", "보완요청 상태가 올바르지 않습니다."),
    // HTTP 400 - 필수 보완서류 누락
    SUPPLEMENT_REQUIRED_DOCUMENT_MISSING(HttpStatus.BAD_REQUEST, "SUPPLEMENT_REQUIRED_DOCUMENT_MISSING", "필수 보완서류가 누락되었습니다."),
    // HTTP 400 - 보완 대상이 아닌 문서 유형
    SUPPLEMENT_DOCUMENT_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SUPPLEMENT_DOCUMENT_TYPE_NOT_ALLOWED", "보완 대상이 아닌 문서 유형입니다."),

    // HTTP 404 - KYC 심사 결과 조회 실패
    KYC_REVIEW_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "KYC_REVIEW_RESULT_NOT_FOUND", "KYC 심사 결과를 찾을 수 없습니다."),
    // HTTP 409 - KYC 완료 화면 조회 불가 상태
    KYC_COMPLETION_NOT_AVAILABLE(HttpStatus.CONFLICT, "KYC_COMPLETION_NOT_AVAILABLE", "KYC 완료 화면을 조회할 수 없는 상태입니다."),
    // HTTP 409 - VC 발급 안내 조회 불가 상태
    CREDENTIAL_GUIDE_NOT_AVAILABLE(HttpStatus.CONFLICT, "CREDENTIAL_GUIDE_NOT_AVAILABLE", "VC 발급 안내를 조회할 수 없습니다."),
    // HTTP 404 - Credential 조회 실패
    CREDENTIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "CREDENTIAL_NOT_FOUND", "Credential을 찾을 수 없습니다."),
    // HTTP 404 - 알림 조회 실패
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
    // HTTP 403 - 알림 리소스 접근 권한 없음
    NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "NOTIFICATION_ACCESS_DENIED", "알림 접근 권한이 없습니다."),
    // HTTP 500 - 감사로그 저장 실패
    AUDIT_LOG_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUDIT_LOG_SAVE_FAILED", "감사로그 저장에 실패했습니다."),
    // HTTP 404 - 모바일 기기 조회 실패
    MOBILE_DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "MOBILE_DEVICE_NOT_FOUND", "모바일 기기를 찾을 수 없습니다."),
    // HTTP 403 - 모바일 기기 리소스 접근 권한 없음
    MOBILE_DEVICE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MOBILE_DEVICE_ACCESS_DENIED", "모바일 기기 접근 권한이 없습니다."),
    // HTTP 409 - 이미 등록된 모바일 기기
    MOBILE_DEVICE_ALREADY_REGISTERED(HttpStatus.CONFLICT, "MOBILE_DEVICE_ALREADY_REGISTERED", "이미 등록된 모바일 기기입니다."),
    // HTTP 400 - 유효하지 않은 모바일 기기 정보
    MOBILE_INVALID_DEVICE(HttpStatus.BAD_REQUEST, "MOBILE_INVALID_DEVICE", "유효하지 않은 모바일 기기 정보입니다."),
    // HTTP 404 - 모바일 보안 설정 조회 실패
    MOBILE_SECURITY_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "MOBILE_SECURITY_SETTING_NOT_FOUND", "모바일 보안 설정을 찾을 수 없습니다."),

    // HTTP 404 - 공통 코드 조회 실패
    COMMON_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_CODE_NOT_FOUND", "공통 코드를 찾을 수 없습니다."),
    // HTTP 404 - 공통 코드 그룹 조회 실패
    COMMON_CODE_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_CODE_GROUP_NOT_FOUND", "공통 코드 그룹을 찾을 수 없습니다.");

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
