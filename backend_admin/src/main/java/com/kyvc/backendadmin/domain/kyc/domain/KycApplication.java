package com.kyvc.backendadmin.domain.kyc.domain;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * kyc_applications 테이블과 매핑되는 엔티티입니다.
 *
 * <p>법인 KYC 신청의 법인/신청자, 상태, AI 심사 상태와 결과, 제출/승인/반려 시각을 보관합니다.</p>
 */
@Entity
@Table(name = "kyc_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KycApplication {

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

    @Column(name = "application_channel_code", length = 30)
    private String applicationChannelCode;

    @Column(name = "finance_institution_code", length = 50)
    private String financeInstitutionCode;

    @Column(name = "finance_branch_code", length = 50)
    private String financeBranchCode;

    @Column(name = "finance_staff_user_id")
    private Long financeStaffUserId;

    @Column(name = "finance_customer_no", length = 100)
    private String financeCustomerNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status_code", nullable = false, length = 50)
    private KyvcEnums.KycStatus kycStatusCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_document_store_option_code", length = 50)
    private KyvcEnums.OriginalDocumentStoreOption originalDocumentStoreOptionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_review_status_code", length = 50)
    private KyvcEnums.AiReviewStatus aiReviewStatusCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_review_result_code", length = 50)
    private KyvcEnums.AiReviewResult aiReviewResultCode;

    @Column(name = "ai_confidence_score", precision = 5, scale = 2)
    private BigDecimal aiConfidenceScore;

    @Column(name = "ai_review_summary", columnDefinition = "TEXT")
    private String aiReviewSummary;

    @Column(name = "manual_review_reason", columnDefinition = "TEXT")
    private String manualReviewReason;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "visited_at")
    private LocalDateTime visitedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
