package com.kyvc.backend.domain.kyc.domain;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

// KYC 신청 엔티티
@Entity
@Table(name = "kyc_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KycApplication {

    private static final Set<KyvcEnums.KycStatus> IN_PROGRESS_STATUSES = Set.of(
            KyvcEnums.KycStatus.DRAFT,
            KyvcEnums.KycStatus.SUBMITTED,
            KyvcEnums.KycStatus.AI_REVIEWING,
            KyvcEnums.KycStatus.NEED_SUPPLEMENT,
            KyvcEnums.KycStatus.MANUAL_REVIEW
    ); // 진행 중 KYC 상태 목록

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kyc_id")
    private Long kycId;

    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;

    @Column(name = "applicant_user_id", nullable = false)
    private Long applicantUserId;

    @Column(name = "corporate_type_code", nullable = false, length = 50)
    private String corporateTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status_code", nullable = false, length = 50)
    private KyvcEnums.KycStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_document_store_option_code", length = 50)
    private KyvcEnums.OriginalDocumentStoreOption originalDocumentStoreOption;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_review_status_code", length = 50)
    private KyvcEnums.AiReviewStatus aiReviewStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_review_result_code", length = 50)
    private KyvcEnums.AiReviewResult aiReviewResult;

    @Column(name = "ai_confidence_score")
    private BigDecimal aiConfidenceScore;

    @Column(name = "ai_review_summary", columnDefinition = "TEXT")
    private String aiReviewSummary;

    @Column(name = "ai_review_detail_json", columnDefinition = "TEXT")
    private String aiReviewDetailJson;

    @Column(name = "manual_review_reason", columnDefinition = "TEXT")
    private String manualReviewReason;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "applied_ai_policy_id")
    private Long appliedAiPolicyId;

    @Column(name = "ai_review_reason_code", length = 100)
    private String aiReviewReasonCode;

    @Column(name = "reject_reason_code", length = 100)
    private String rejectReasonCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // KYC 초안 생성
    public static KycApplication createDraft(
            Long corporateId, // 법인 ID
            Long applicantUserId, // 신청 사용자 ID
            String corporateTypeCode // 법인 유형 코드
    ) {
        KycApplication kycApplication = new KycApplication();
        kycApplication.corporateId = corporateId;
        kycApplication.applicantUserId = applicantUserId;
        kycApplication.corporateTypeCode = corporateTypeCode;
        kycApplication.kycStatus = KyvcEnums.KycStatus.DRAFT;
        return kycApplication;
    }

    public Long getKycId() {
        return kycId;
    }

    public Long getCorporateId() {
        return corporateId;
    }

    public Long getApplicantUserId() {
        return applicantUserId;
    }

    public String getCorporateTypeCode() {
        return corporateTypeCode;
    }

    public KyvcEnums.KycStatus getKycStatus() {
        return kycStatus;
    }

    public KyvcEnums.OriginalDocumentStoreOption getOriginalDocumentStoreOption() {
        return originalDocumentStoreOption;
    }

    public KyvcEnums.AiReviewStatus getAiReviewStatus() {
        return aiReviewStatus;
    }

    public KyvcEnums.AiReviewResult getAiReviewResult() {
        return aiReviewResult;
    }

    public BigDecimal getAiConfidenceScore() {
        return aiConfidenceScore;
    }

    public String getAiReviewSummary() {
        return aiReviewSummary;
    }

    public String getAiReviewDetailJson() {
        return aiReviewDetailJson;
    }

    public String getManualReviewReason() {
        return manualReviewReason;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public Long getAppliedAiPolicyId() {
        return appliedAiPolicyId;
    }

    public String getAiReviewReasonCode() {
        return aiReviewReasonCode;
    }

    public String getRejectReasonCode() {
        return rejectReasonCode;
    }

    // 법인 유형 변경
    public void changeCorporateType(
            String corporateTypeCode // 법인 유형 코드
    ) {
        this.corporateTypeCode = corporateTypeCode;
    }

    // 원본서류 저장 옵션 변경
    public void changeDocumentStoreOption(
            KyvcEnums.OriginalDocumentStoreOption option // 원본서류 저장 옵션
    ) {
        this.originalDocumentStoreOption = option;
    }

    // KYC 제출 처리
    public void submit(
            LocalDateTime submittedAt // 제출 일시
    ) {
        this.kycStatus = KyvcEnums.KycStatus.SUBMITTED;
        this.submittedAt = submittedAt;
    }

    // 보완 제출 처리
    public void submitSupplement(
            LocalDateTime submittedAt // 제출 일시
    ) {
        this.kycStatus = KyvcEnums.KycStatus.SUBMITTED;
        this.submittedAt = submittedAt;
    }

    // AI 심사 시작 처리
    public void startAiReview(
            LocalDateTime submittedAt // 제출 일시
    ) {
        this.kycStatus = KyvcEnums.KycStatus.AI_REVIEWING;
        this.aiReviewStatus = KyvcEnums.AiReviewStatus.QUEUED;
        this.submittedAt = submittedAt;
    }

    // AI 심사 완료 후 수동심사 전환
    public void completeAiReviewAsManualReview(
            BigDecimal confidenceScore, // AI 신뢰도 점수
            String summary, // AI 심사 요약
            String detailJson, // AI 심사 상세 JSON
            String manualReviewReason // 수동심사 전환 사유
    ) {
        this.kycStatus = KyvcEnums.KycStatus.MANUAL_REVIEW;
        this.aiReviewStatus = KyvcEnums.AiReviewStatus.SUCCESS;
        this.aiReviewResult = KyvcEnums.AiReviewResult.NEED_MANUAL_REVIEW;
        this.aiConfidenceScore = confidenceScore;
        this.aiReviewSummary = summary;
        this.aiReviewDetailJson = detailJson;
        this.manualReviewReason = manualReviewReason;
    }

    // Credential 발급 가능 상태 여부
    public boolean isCredentialIssuable() {
        return KyvcEnums.KycStatus.APPROVED == kycStatus;
    }

    // VC 발급 완료 상태 반영
    public void markVcIssued(
            LocalDateTime issuedAt // VC 발급 일시
    ) {
        this.kycStatus = KyvcEnums.KycStatus.VC_ISSUED;
        if (this.approvedAt == null) {
            this.approvedAt = issuedAt;
        }
    }

    // 개발/E2E 테스트용 임시 승인 가능 여부
    public boolean canApproveForDevTest() {
        return KyvcEnums.KycStatus.SUBMITTED == kycStatus
                || KyvcEnums.KycStatus.AI_REVIEWING == kycStatus
                || KyvcEnums.KycStatus.MANUAL_REVIEW == kycStatus
                || KyvcEnums.KycStatus.NEED_SUPPLEMENT == kycStatus;
    }

    // 개발/E2E 테스트용 임시 승인 처리
    public void approveForDevTest(
            LocalDateTime approvedAt // 임시 승인 일시
    ) {
        this.kycStatus = KyvcEnums.KycStatus.APPROVED;
        this.approvedAt = approvedAt;
    }

    // AI 심사 실패 후 수동심사 전환
    public void failAiReviewAsManualReview(
            String manualReviewReason // 수동심사 전환 사유
    ) {
        this.kycStatus = KyvcEnums.KycStatus.MANUAL_REVIEW;
        this.aiReviewStatus = KyvcEnums.AiReviewStatus.FAILED;
        this.manualReviewReason = manualReviewReason;
    }

    // 소유 여부
    public boolean isOwnedBy(
            Long userId // 사용자 ID
    ) {
        return applicantUserId != null && applicantUserId.equals(userId);
    }

    // 초안 상태 여부
    public boolean isDraft() {
        return KyvcEnums.KycStatus.DRAFT == kycStatus;
    }

    // 문서 업로드 가능 여부
    public boolean isDocumentUploadAllowed() {
        return KyvcEnums.KycStatus.DRAFT == kycStatus
                || KyvcEnums.KycStatus.NEED_SUPPLEMENT == kycStatus;
    }

    // 진행 중 KYC 여부
    public boolean isInProgress() {
        return IN_PROGRESS_STATUSES.contains(kycStatus);
    }
}
