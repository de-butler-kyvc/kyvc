package com.kyvc.backend.domain.verifier.domain;

import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Verifier 연동 로그 Entity
@Entity
@Table(name = "verifier_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerifierLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verifier_log_id")
    private Long verifierLogId; // Verifier 로그 ID

    @Column(name = "verifier_id", nullable = false)
    private Long verifierId; // Verifier ID

    @Column(name = "api_key_id")
    private Long apiKeyId; // API Key ID

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type_code", nullable = false, length = 50)
    private KyvcEnums.VerifierActionType actionTypeCode; // 작업 유형

    @Column(name = "request_path", length = 500)
    private String requestPath; // 요청 경로

    @Column(name = "method", length = 20)
    private String method; // HTTP method

    @Column(name = "status_code")
    private Integer statusCode; // HTTP 상태

    @Column(name = "result_code", length = 50)
    private String resultCode; // 처리 결과

    @Column(name = "latency_ms")
    private Long latencyMs; // 처리 시간

    @Column(name = "client_sdk_version", length = 50)
    private String clientSdkVersion; // Client SDK 버전

    @Column(name = "policy_version", length = 50)
    private String policyVersion; // 정책 버전

    @Column(name = "error_message", length = 500)
    private String errorMessage; // 오류 요약

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt; // 요청 일시

    // Verifier 로그 생성
    public static VerifierLog create(
            Long verifierId, // Verifier ID
            Long apiKeyId, // API Key ID
            KyvcEnums.VerifierActionType actionTypeCode, // 작업 유형
            String requestPath, // 요청 경로
            String method, // HTTP method
            Integer statusCode, // HTTP 상태
            String resultCode, // 처리 결과
            String errorMessage // 오류 요약
    ) {
        VerifierLog verifierLog = new VerifierLog();
        verifierLog.verifierId = verifierId;
        verifierLog.apiKeyId = apiKeyId;
        verifierLog.actionTypeCode = actionTypeCode;
        verifierLog.requestPath = requestPath;
        verifierLog.method = method;
        verifierLog.statusCode = statusCode;
        verifierLog.resultCode = resultCode;
        verifierLog.errorMessage = errorMessage;
        verifierLog.requestedAt = LocalDateTime.now();
        return verifierLog;
    }
}
