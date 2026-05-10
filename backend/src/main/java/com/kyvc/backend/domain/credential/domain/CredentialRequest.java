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
// Credential 요청 이력 엔티티
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
    private KyvcEnums.CredentialRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status_code", nullable = false, length = 30)
    private KyvcEnums.CredentialRequestStatus requestStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by_type_code", nullable = false, length = 30)
    private KyvcEnums.ActorType requestedByType;

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
        return create(credentialId, requestTypeCode, requestedByTypeCode, requestedById, null, reason);
    }

    // Credential 요청 이력 생성
    public static CredentialRequest create(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialRequestType requestType, // 요청 유형
            KyvcEnums.ActorType requestedByType, // 요청자 유형
            Long requestedById, // 요청자 ID
            String reasonCode, // 요청 사유 코드
            String reason // 요청 사유
    ) {
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.credentialId = credentialId;
        credentialRequest.requestType = requestType;
        credentialRequest.requestStatus = KyvcEnums.CredentialRequestStatus.REQUESTED;
        credentialRequest.requestedByType = requestedByType;
        credentialRequest.requestedById = requestedById;
        credentialRequest.reasonCode = reasonCode;
        credentialRequest.reason = reason;
        credentialRequest.requestedAt = LocalDateTime.now();
        return credentialRequest;
    }

    // 처리 중 상태 반영
    // Core 처리 시작 반영
    public void markProcessing(
            String coreRequestId // Core 요청 ID
    ) {
        this.coreRequestId = coreRequestId;
        this.requestStatus = KyvcEnums.CredentialRequestStatus.PROCESSING;
    }

    // 완료 상태 반영
    public void markCompleted() {
        markCompleted(null);
    }

    // 요청 완료 반영
    public void markCompleted(
            String reasonCode // 완료 사유 코드
    ) {
        this.requestStatus = KyvcEnums.CredentialRequestStatus.COMPLETED;
        this.reasonCode = reasonCode;
        this.completedAt = LocalDateTime.now();
    }

    // 실패 상태 반영
    public void markFailed() {
        markFailed(null);
    }

    // 요청 실패 반영
    public void markFailed(
            String reasonCode // 실패 사유 코드
    ) {
        this.requestStatus = KyvcEnums.CredentialRequestStatus.FAILED;
        this.reasonCode = reasonCode;
        this.completedAt = LocalDateTime.now();
    }

    // 진행 중 상태 여부
    // 진행 중 요청 여부
    public boolean isInProgress() {
        return IN_PROGRESS_STATUSES.contains(requestStatus);
    }

    public KyvcEnums.CredentialRequestType getRequestTypeCode() {
        return requestType;
    }

    public KyvcEnums.CredentialRequestStatus getRequestStatusCode() {
        return requestStatus;
    }

    public KyvcEnums.ActorType getRequestedByTypeCode() {
        return requestedByType;
    }
}
