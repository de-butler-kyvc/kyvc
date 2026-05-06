package com.kyvc.backend.domain.review.domain;

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

// KYC 보완요청 엔티티
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

    @Column(name = "request_reason", nullable = false)
    private String requestReason;

    @Column(name = "requested_document_type_codes", nullable = false)
    private String requestedDocumentTypeCodes;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "supplement_reason_code", length = 100)
    private String supplementReasonCode;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "message")
    private String message;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "submitted_comment")
    private String submittedComment;

    // KYC 소속 여부
    public boolean belongsToKyc(
            Long kycId // KYC 요청 ID
    ) {
        return this.kycId != null && this.kycId.equals(kycId);
    }

    // 요청 상태 여부
    public boolean isRequested() {
        return KyvcEnums.SupplementStatus.REQUESTED == supplementStatus;
    }

    // 보완 제출 처리
    public void submit(
            String submittedComment, // 제출 코멘트
            LocalDateTime submittedAt // 제출 일시
    ) {
        this.supplementStatus = KyvcEnums.SupplementStatus.SUBMITTED;
        this.submittedComment = submittedComment;
        this.completedAt = submittedAt;
    }
}
