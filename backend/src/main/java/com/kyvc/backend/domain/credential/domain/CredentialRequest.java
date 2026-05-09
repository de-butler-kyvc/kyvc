package com.kyvc.backend.domain.credential.domain;

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
import java.util.Set;

// Credential 업무 요청 Entity
@Entity
@Table(name = "credential_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CredentialRequest {

    private static final Set<KyvcEnums.CredentialRequestStatus> IN_PROGRESS_STATUSES = Set.of(
            KyvcEnums.CredentialRequestStatus.REQUESTED,
            KyvcEnums.CredentialRequestStatus.PROCESSING
    ); // 진행 중 요청 상태 목록

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credential_request_id")
    private Long credentialRequestId;

    @Column(name = "credential_id", nullable = false)
    private Long credentialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type_code", nullable = false, length = 30)
    private KyvcEnums.CredentialRequestType requestTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status_code", nullable = false, length = 30)
    private KyvcEnums.CredentialRequestStatus requestStatusCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by_type_code", nullable = false, length = 30)
    private KyvcEnums.ActorType requestedByTypeCode;

    @Column(name = "requested_by_id")
    private Long requestedById;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "core_request_id", length = 255)
    private String coreRequestId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Credential 요청 생성
    public static CredentialRequest create(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialRequestType requestTypeCode, // 요청 유형
            KyvcEnums.ActorType requestedByTypeCode, // 요청자 유형
            Long requestedById, // 요청자 ID
            String reason // 요청 사유
    ) {
        CredentialRequest request = new CredentialRequest();
        request.credentialId = credentialId;
        request.requestTypeCode = requestTypeCode;
        request.requestStatusCode = KyvcEnums.CredentialRequestStatus.REQUESTED;
        request.requestedByTypeCode = requestedByTypeCode;
        request.requestedById = requestedById;
        request.reason = reason;
        request.requestedAt = LocalDateTime.now();
        return request;
    }

    // 처리 중 상태 반영
    public void markProcessing(
            String coreRequestId // Core 요청 ID
    ) {
        this.coreRequestId = coreRequestId;
        this.requestStatusCode = KyvcEnums.CredentialRequestStatus.PROCESSING;
    }

    // 완료 상태 반영
    public void markCompleted() {
        this.requestStatusCode = KyvcEnums.CredentialRequestStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    // 실패 상태 반영
    public void markFailed() {
        this.requestStatusCode = KyvcEnums.CredentialRequestStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    // 진행 중 상태 여부
    public boolean isInProgress() {
        return IN_PROGRESS_STATUSES.contains(requestStatusCode);
    }
}
