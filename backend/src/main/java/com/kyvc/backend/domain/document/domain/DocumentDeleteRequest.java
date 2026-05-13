package com.kyvc.backend.domain.document.domain;

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

// 문서 삭제 요청 Entity
@Entity
@Table(name = "document_delete_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentDeleteRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId; // 삭제 요청 ID

    @Column(name = "document_id", nullable = false)
    private Long documentId; // 문서 ID

    @Column(name = "requested_by_user_id", nullable = false)
    private Long requestedByUserId; // 요청 사용자 ID

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status_code", nullable = false, length = 30)
    private KyvcEnums.DocumentDeleteRequestStatus requestStatusCode; // 삭제 요청 상태 코드

    @Column(name = "request_reason", columnDefinition = "TEXT")
    private String requestReason; // 요청 사유

    @Column(name = "processed_by_admin_id")
    private Long processedByAdminId; // 처리 관리자 ID

    @Column(name = "processed_reason", columnDefinition = "TEXT")
    private String processedReason; // 처리 사유

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt; // 요청 일시

    @Column(name = "processed_at")
    private LocalDateTime processedAt; // 처리 일시

    // 삭제 요청 생성
    public static DocumentDeleteRequest createRequested(
            Long documentId, // 문서 ID
            Long requestedByUserId, // 요청 사용자 ID
            String requestReason // 요청 사유
    ) {
        DocumentDeleteRequest request = new DocumentDeleteRequest();
        request.documentId = documentId;
        request.requestedByUserId = requestedByUserId;
        request.requestStatusCode = KyvcEnums.DocumentDeleteRequestStatus.REQUESTED;
        request.requestReason = requestReason;
        request.requestedAt = LocalDateTime.now();
        return request;
    }
}
