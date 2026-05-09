package com.kyvc.backendadmin.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Verifier 관리자 API에서 사용하는 DTO 모음입니다.
 */
public final class AdminVerifierDtos {

    private AdminVerifierDtos() {
    }

    @Schema(description = "Verifier 등록 요청")
    public record CreateRequest(
            /** Verifier 플랫폼명 */
            @NotBlank(message = "name은 필수입니다.")
            @Schema(description = "Verifier 플랫폼명", example = "Partner Verifier")
            String name,
            /** 사업자등록번호. V11 DB에는 별도 컬럼이 없어 중복 검증 참고값으로만 사용한다. */
            @Schema(description = "사업자등록번호. V11 DB에는 별도 컬럼이 없어 저장하지 않는다.", example = "123-45-67890")
            String businessNo,
            /** Callback URL */
            @Schema(description = "Callback URL", example = "https://partner.example.com/callback")
            String callbackUrl,
            /** 담당자 이메일 */
            @Schema(description = "담당자 이메일", example = "manager@example.com")
            String managerEmail,
            /** 담당자명. V11 DB에는 별도 컬럼이 없어 저장하지 않는다. */
            @Schema(description = "담당자명. V11 DB에는 별도 컬럼이 없어 저장하지 않는다.", example = "담당자")
            String managerName
    ) {
    }

    @Schema(description = "Verifier 수정 요청")
    public record UpdateRequest(
            /** Verifier 플랫폼명 */
            @Schema(description = "Verifier 플랫폼명", example = "Partner Verifier")
            String name,
            /** 담당자 이메일 */
            @Schema(description = "담당자 이메일", example = "manager@example.com")
            String managerEmail,
            /** 담당자명. V11 DB에는 별도 컬럼이 없어 저장하지 않는다. */
            @Schema(description = "담당자명. V11 DB에는 별도 컬럼이 없어 저장하지 않는다.", example = "담당자")
            String managerName,
            /** Callback URL */
            @Schema(description = "Callback URL", example = "https://partner.example.com/vp/callback")
            String callbackUrl
    ) {
    }

    @Schema(description = "Verifier 승인 요청")
    public record ApproveRequest(
            /** 승인 코멘트 */
            @Schema(description = "승인 코멘트", example = "심사 완료")
            String comment,
            /** MFA 세션 토큰 */
            @NotBlank(message = "mfaToken은 필수입니다.")
            @Schema(description = "MFA 세션 토큰", example = "mfa_session_token")
            String mfaToken
    ) {
    }

    @Schema(description = "Verifier 중지 요청")
    public record SuspendRequest(
            /** 중지 사유 코드 */
            @Schema(description = "중지 사유 코드", example = "POLICY_VIOLATION")
            String reasonCode,
            /** 중지 코멘트 */
            @Schema(description = "중지 코멘트", example = "정책 위반")
            String comment,
            /** MFA 세션 토큰 */
            @NotBlank(message = "mfaToken은 필수입니다.")
            @Schema(description = "MFA 세션 토큰", example = "mfa_session_token")
            String mfaToken
    ) {
    }

    @Schema(description = "Verifier 응답")
    public record Response(
            /** Verifier ID */
            @Schema(description = "Verifier ID", example = "1")
            Long verifierId,
            /** Verifier 플랫폼명 */
            @Schema(description = "Verifier 플랫폼명", example = "Partner Verifier")
            String name,
            /** Verifier 상태 코드 */
            @Schema(description = "Verifier 상태 코드", example = "PENDING")
            String status,
            /** 담당자 이메일 */
            @Schema(description = "담당자 이메일", example = "manager@example.com")
            String managerEmail,
            /** 승인 시각 */
            @Schema(description = "승인 시각")
            LocalDateTime approvedAt,
            /** 중지 시각 */
            @Schema(description = "중지 시각")
            LocalDateTime suspendedAt,
            /** 생성 시각 */
            @Schema(description = "생성 시각")
            LocalDateTime createdAt,
            /** 수정 시각 */
            @Schema(description = "수정 시각")
            LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "Verifier 목록 응답")
    public record PageResponse(
            /** Verifier 목록 */
            @Schema(description = "Verifier 목록")
            List<Response> items,
            /** 현재 페이지 번호 */
            @Schema(description = "현재 페이지 번호", example = "0")
            int page,
            /** 페이지 크기 */
            @Schema(description = "페이지 크기", example = "20")
            int size,
            /** 전체 건수 */
            @Schema(description = "전체 건수", example = "120")
            long totalElements,
            /** 전체 페이지 수 */
            @Schema(description = "전체 페이지 수", example = "6")
            int totalPages
    ) {
    }

    @Schema(description = "Verifier 상세 응답")
    public record DetailResponse(
            /** Verifier 기본정보 */
            @Schema(description = "Verifier 기본정보")
            Response verifier,
            /** Callback 목록 */
            @Schema(description = "Callback 목록")
            List<CallbackResponse> callbacks,
            /** API Key 요약 목록 */
            @Schema(description = "API Key 요약 목록")
            List<ApiKeyResponse> apiKeys,
            /** 최근 사용량 통계 */
            @Schema(description = "최근 사용량 통계")
            UsageStatsResponse usageStats
    ) {
    }

    @Schema(description = "API Key 발급 요청")
    public record ApiKeyCreateRequest(
            /** API Key 이름 */
            @NotBlank(message = "name은 필수입니다.")
            @Schema(description = "API Key 이름", example = "운영 SDK Key")
            String name,
            /** 만료 시각 */
            @Schema(description = "만료 시각", example = "2026-12-31T23:59:59")
            LocalDateTime expiresAt,
            /** MFA 세션 토큰 */
            @NotBlank(message = "mfaToken은 필수입니다.")
            @Schema(description = "MFA 세션 토큰", example = "mfa_session_token")
            String mfaToken
    ) {
    }

    @Schema(description = "API Key 폐기 요청")
    public record ApiKeyRotateRequest(
            /** MFA 세션 토큰 */
            @NotBlank(message = "mfaToken은 필수입니다.")
            @Schema(description = "MFA 세션 토큰", example = "mfa_session_token")
            String mfaToken
    ) {
    }

    public record ApiKeyRevokeRequest(
            /** 폐기 사유 */
            @Schema(description = "폐기 사유", example = "키 노출 의심")
            String reason,
            /** MFA 세션 토큰 */
            @NotBlank(message = "mfaToken은 필수입니다.")
            @Schema(description = "MFA 세션 토큰", example = "mfa_session_token")
            String mfaToken
    ) {
    }

    @Schema(description = "API Key 응답")
    public record ApiKeyResponse(
            /** API Key ID */
            @Schema(description = "API Key ID", example = "1")
            Long keyId,
            /** API Key 이름 */
            @Schema(description = "API Key 이름", example = "운영 SDK Key")
            String keyName,
            /** API Key prefix */
            @Schema(description = "API Key prefix", example = "kyvc_live_abcd1234")
            String keyPrefix,
            /** API Key 상태 코드 */
            @Schema(description = "API Key 상태 코드", example = "ACTIVE")
            String keyStatusCode,
            /** 발급 시각 */
            @Schema(description = "발급 시각")
            LocalDateTime issuedAt,
            /** 만료 시각 */
            @Schema(description = "만료 시각")
            LocalDateTime expiresAt,
            /** 마지막 사용 시각 */
            @Schema(description = "마지막 사용 시각")
            LocalDateTime lastUsedAt,
            /** 생성 시각 */
            @Schema(description = "생성 시각")
            LocalDateTime createdAt
    ) {
    }

    @Schema(description = "API Key 최초 발급/회전 응답")
    public record ApiKeySecretResponse(
            /** API Key ID */
            @Schema(description = "API Key ID", example = "1")
            Long keyId,
            /** API Key prefix */
            @Schema(description = "API Key prefix", example = "kyvc_live_abcd1234")
            String keyPrefix,
            /** 최초 1회만 반환되는 API Key 원문 */
            @Schema(description = "최초 1회만 반환되는 API Key 원문", example = "kyvc_live_abcd1234.secret")
            String secret,
            /** 만료 시각 */
            @Schema(description = "만료 시각")
            LocalDateTime expiresAt
    ) {
    }

    @Schema(description = "Callback 설정 요청")
    public record CallbackUpdateRequest(
            /** Callback URL */
            @NotBlank(message = "callbackUrl은 필수입니다.")
            @Schema(description = "Callback URL", example = "https://partner.example.com/vp/callback")
            String callbackUrl,
            /** Callback 상태 코드 */
            @Schema(description = "Callback 상태 코드", example = "ACTIVE")
            String callbackStatusCode
    ) {
    }

    @Schema(description = "Callback 응답")
    public record CallbackResponse(
            /** Callback ID */
            @Schema(description = "Callback ID", example = "1")
            Long callbackId,
            /** Verifier ID */
            @Schema(description = "Verifier ID", example = "1")
            Long verifierId,
            /** Callback URL */
            @Schema(description = "Callback URL")
            String callbackUrl,
            /** Callback 상태 코드 */
            @Schema(description = "Callback 상태 코드", example = "ACTIVE")
            String callbackStatusCode,
            /** 사용 여부 */
            @Schema(description = "사용 여부", example = "Y")
            String enabledYn,
            /** 생성 시각 */
            @Schema(description = "생성 시각")
            LocalDateTime createdAt,
            /** 수정 시각 */
            @Schema(description = "수정 시각")
            LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "Verifier 로그 검색 조건")
    public record LogSearchRequest(
            /** Verifier ID */
            @Schema(description = "Verifier ID", example = "1")
            Long verifierId,
            /** Action 유형 코드 */
            @Schema(description = "Action 유형 코드", example = "VP_VERIFY")
            String actionTypeCode,
            /** HTTP 상태 코드 */
            @Schema(description = "HTTP 상태 코드", example = "200")
            Integer statusCode,
            /** 조회 시작일 */
            @Schema(description = "조회 시작일", example = "2026-05-01")
            LocalDate fromDate,
            /** 조회 종료일 */
            @Schema(description = "조회 종료일", example = "2026-05-31")
            LocalDate toDate,
            /** 페이지 번호 */
            @Schema(description = "페이지 번호", example = "0")
            int page,
            /** 페이지 크기 */
            @Schema(description = "페이지 크기", example = "20")
            int size
    ) {
        public static LogSearchRequest of(Long verifierId, String actionTypeCode, Integer statusCode,
                                          LocalDate fromDate, LocalDate toDate, Integer page, Integer size) {
            return new LogSearchRequest(verifierId, actionTypeCode, statusCode, fromDate, toDate,
                    page == null || page < 0 ? 0 : page, size == null || size < 1 ? 20 : Math.min(size, 100));
        }
    }

    @Schema(description = "Verifier 로그 응답")
    public record LogResponse(
            /** 로그 ID */
            @Schema(description = "로그 ID", example = "1")
            Long logId,
            /** Verifier ID */
            @Schema(description = "Verifier ID", example = "1")
            Long verifierId,
            /** API Key ID */
            @Schema(description = "API Key ID", example = "1")
            Long apiKeyId,
            /** Action 유형 코드 */
            @Schema(description = "Action 유형 코드", example = "VP_VERIFY")
            String actionTypeCode,
            /** 요청 경로 */
            @Schema(description = "요청 경로")
            String requestPath,
            /** HTTP 메서드 */
            @Schema(description = "HTTP 메서드", example = "POST")
            String method,
            /** HTTP 상태 코드 */
            @Schema(description = "HTTP 상태 코드", example = "200")
            Integer statusCode,
            /** 결과 코드 */
            @Schema(description = "결과 코드", example = "SUCCESS")
            String resultCode,
            /** 지연 시간 ms */
            @Schema(description = "지연 시간 ms", example = "120")
            Integer latencyMs,
            /** SDK 버전 */
            @Schema(description = "SDK 버전")
            String clientSdkVersion,
            /** 정책 버전 */
            @Schema(description = "정책 버전")
            String policyVersion,
            /** 오류 메시지 */
            @Schema(description = "오류 메시지")
            String errorMessage,
            /** 요청 시각 */
            @Schema(description = "요청 시각")
            LocalDateTime requestedAt
    ) {
    }

    @Schema(description = "Verifier 로그 페이지 응답")
    public record LogPageResponse(List<LogResponse> items, int page, int size, long totalElements, int totalPages) {
    }

    @Schema(description = "Verifier 사용량 통계 응답")
    public record UsageStatsResponse(
            /** VP 검증 요청 수 */
            @Schema(description = "VP 검증 요청 수", example = "10")
            long vpRequestCount,
            /** VP 검증 성공 수 */
            @Schema(description = "VP 검증 성공 수", example = "8")
            long successCount,
            /** VP 검증 실패 수 */
            @Schema(description = "VP 검증 실패 수", example = "2")
            long failedCount,
            /** Callback 성공 수 */
            @Schema(description = "Callback 성공 수", example = "7")
            long callbackSuccessCount,
            /** Callback 실패 수 */
            @Schema(description = "Callback 실패 수", example = "1")
            long callbackFailedCount,
            /** API Key 사용 수 */
            @Schema(description = "API Key 사용 수", example = "15")
            long apiKeyUsageCount,
            /** 기간별 집계 */
            @Schema(description = "기간별 집계")
            List<DailyUsage> dailyUsages
    ) {
    }

    @Schema(description = "Verifier 일자별 사용량")
    public record DailyUsage(
            /** 집계 일자 */
            @Schema(description = "집계 일자", example = "2026-05-09")
            LocalDate date,
            /** 요청 수 */
            @Schema(description = "요청 수", example = "5")
            long requestCount,
            /** 성공 수 */
            @Schema(description = "성공 수", example = "4")
            long successCount,
            /** 실패 수 */
            @Schema(description = "실패 수", example = "1")
            long failedCount
    ) {
    }
}
