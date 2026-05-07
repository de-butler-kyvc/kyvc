package com.kyvc.backendadmin.domain.review.domain;

import com.kyvc.backendadmin.global.util.KyvcEnums;
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
import java.util.List;

/**
 * kyc_supplements 테이블과 매핑되는 보완요청 엔티티입니다.
 */
@Entity
@Table(name = "kyc_supplements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KycSupplement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplement_id")
    private Long supplementId;

    @Column(name = "kyc_id", nullable = false)
    private Long kycId;

    @Column(name = "requested_by_admin_id", nullable = false)
    private Long requestedByAdminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplement_status_code", nullable = false, length = 50)
    private KyvcEnums.SupplementStatus supplementStatus;

    @Column(name = "request_reason", nullable = false, columnDefinition = "TEXT")
    private String requestReason;

    @Column(name = "requested_document_type_codes", nullable = false, columnDefinition = "TEXT")
    private String requestedDocumentTypeCodes;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "supplement_reason_code", length = 100)
    private String supplementReasonCode;

    @Column(name = "title")
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "submitted_comment", columnDefinition = "TEXT")
    private String submittedComment;

    public static KycSupplement create(
            Long kycId,
            Long requestedByAdminId,
            String supplementReasonCode,
            String title,
            String message,
            List<String> requestedDocumentTypes,
            LocalDateTime dueAt
    ) {
        KycSupplement supplement = new KycSupplement();
        supplement.kycId = kycId;
        supplement.requestedByAdminId = requestedByAdminId;
        supplement.supplementStatus = KyvcEnums.SupplementStatus.REQUESTED;
        supplement.requestReason = message;
        supplement.requestedDocumentTypeCodes = String.join(",", requestedDocumentTypes);
        supplement.requestedAt = LocalDateTime.now();
        supplement.supplementReasonCode = supplementReasonCode;
        supplement.title = title;
        supplement.message = message;
        supplement.dueAt = dueAt;
        return supplement;
    }
}
