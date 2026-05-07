package com.kyvc.backend.domain.core.domain;

import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

// Core 요청 추적 Entity
@Entity
@Table(name = "core_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoreRequest {

    @Id
    @Column(name = "core_request_id", nullable = false, length = 255)
    private String coreRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "core_request_type_code", nullable = false, length = 50)
    private KyvcEnums.CoreRequestType coreRequestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "core_target_type_code", nullable = false, length = 50)
    private KyvcEnums.CoreTargetType coreTargetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "core_request_status_code", nullable = false, length = 50)
    private KyvcEnums.CoreRequestStatus coreRequestStatus;

    @Column(name = "request_payload_json", columnDefinition = "TEXT")
    private String requestPayloadJson;

    @Column(name = "response_payload_json", columnDefinition = "TEXT")
    private String responsePayloadJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Core 요청 생성
    public static CoreRequest create(
            KyvcEnums.CoreRequestType coreRequestType, // Core 요청 유형
            KyvcEnums.CoreTargetType coreTargetType, // Core 대상 유형
            Long targetId, // Core 대상 ID
            String requestPayloadJson // 요청 Payload JSON
    ) {
        CoreRequest coreRequest = new CoreRequest();
        coreRequest.coreRequestId = UUID.randomUUID().toString();
        coreRequest.coreRequestType = coreRequestType;
        coreRequest.coreTargetType = coreTargetType;
        coreRequest.targetId = targetId;
        coreRequest.coreRequestStatus = KyvcEnums.CoreRequestStatus.QUEUED;
        coreRequest.requestPayloadJson = requestPayloadJson;
        coreRequest.retryCount = 0;
        coreRequest.requestedAt = LocalDateTime.now();
        return coreRequest;
    }

    // 요청 Payload JSON 갱신
    public void updateRequestPayloadJson(
            String requestPayloadJson // 요청 Payload JSON
    ) {
        this.requestPayloadJson = requestPayloadJson;
    }

    // 요청 완료 응답 반영
    public void markRequested(
            String responsePayloadJson // 요청 응답 Payload JSON
    ) {
        this.coreRequestStatus = KyvcEnums.CoreRequestStatus.REQUESTED;
        this.responsePayloadJson = responsePayloadJson;
    }

    // Callback 수신 상태 반영
    public void markCallbackReceived() {
        this.coreRequestStatus = KyvcEnums.CoreRequestStatus.CALLBACK_RECEIVED;
    }

    // Callback 성공 상태 반영
    public void markSuccess(
            String responsePayloadJson // 성공 응답 Payload JSON
    ) {
        this.coreRequestStatus = KyvcEnums.CoreRequestStatus.SUCCESS;
        this.responsePayloadJson = responsePayloadJson;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    // Callback 실패 상태 반영
    public void markFailed(
            String errorMessage // 실패 메시지
    ) {
        this.coreRequestStatus = KyvcEnums.CoreRequestStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    // 재시도 횟수 증가
    public void increaseRetryCount() {
        this.retryCount = retryCount == null ? 1 : retryCount + 1;
        this.coreRequestStatus = KyvcEnums.CoreRequestStatus.RETRYING;
    }

    // 완료 상태 여부
    public boolean isCompleted() {
        return KyvcEnums.CoreRequestStatus.SUCCESS == coreRequestStatus
                || KyvcEnums.CoreRequestStatus.FAILED == coreRequestStatus;
    }

    // 동일 대상 여부
    public boolean isSameTarget(
            KyvcEnums.CoreTargetType targetType, // 대상 유형
            Long targetId // 대상 ID
    ) {
        return coreTargetType == targetType
                && this.targetId != null
                && this.targetId.equals(targetId);
    }
}
