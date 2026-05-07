package com.kyvc.backendadmin.domain.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * ai_review_policies 테이블과 매핑되는 AI 심사 업무 정책 엔티티입니다.
 */
@Entity
@Table(name = "ai_review_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReviewPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_policy_id")
    private Long aiPolicyId;

    @Column(name = "policy_name", nullable = false)
    private String policyName;

    @Column(name = "corporate_type_code", nullable = false, length = 50)
    private String corporateTypeCode;

    @Column(name = "auto_approve_enabled_yn", nullable = false, length = 1)
    private String autoApproveYn;

    @Column(name = "auto_approve_min_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal autoApproveThreshold;

    @Column(name = "manual_review_below_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal manualReviewThreshold;

    @Column(name = "supplement_below_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal supplementThreshold;

    @Column(name = "mismatch_action_code", nullable = false, length = 50)
    private String mismatchActionCode;

    @Column(name = "missing_required_field_action_code", nullable = false, length = 50)
    private String missingRequiredFieldActionCode;

    @Column(name = "delegation_issue_action_code", nullable = false, length = 50)
    private String delegationIssueActionCode;

    @Column(name = "beneficial_owner_issue_action_code", nullable = false, length = 50)
    private String beneficialOwnerIssueActionCode;

    @Column(name = "ai_failure_action_code", nullable = false, length = 50)
    private String aiFailureActionCode;

    @Column(name = "enabled_yn", nullable = false, length = 1)
    private String enabledYn;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "updated_by_admin_id")
    private Long updatedByAdminId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * AI 심사 업무 정책을 생성합니다.
     *
     * @param policyName 정책명
     * @param corporateTypeCode 법인 유형 코드
     * @param autoApproveYn 자동 승인 사용 여부
     * @param autoApproveThreshold 자동 승인 기준
     * @param manualReviewThreshold 수동 심사 기준
     * @param supplementThreshold 보완요청 후보 기준
     * @param adminId 생성 관리자 ID
     * @return AI 심사 업무 정책 엔티티
     */
    public static AiReviewPolicy create(
            String policyName,
            String corporateTypeCode,
            String autoApproveYn,
            BigDecimal autoApproveThreshold,
            BigDecimal manualReviewThreshold,
            BigDecimal supplementThreshold,
            Long adminId
    ) {
        AiReviewPolicy policy = new AiReviewPolicy();
        policy.policyName = policyName;
        policy.corporateTypeCode = corporateTypeCode;
        policy.autoApproveYn = autoApproveYn;
        policy.autoApproveThreshold = autoApproveThreshold;
        policy.manualReviewThreshold = manualReviewThreshold;
        policy.supplementThreshold = supplementThreshold;
        policy.mismatchActionCode = "MANUAL_REVIEW";
        policy.missingRequiredFieldActionCode = "NEED_SUPPLEMENT";
        policy.delegationIssueActionCode = "MANUAL_REVIEW";
        policy.beneficialOwnerIssueActionCode = "MANUAL_REVIEW";
        policy.aiFailureActionCode = "MANUAL_REVIEW";
        policy.enabledYn = "Y";
        policy.effectiveFrom = LocalDateTime.now();
        policy.createdByAdminId = adminId;
        policy.updatedByAdminId = adminId;
        return policy;
    }

    /**
     * AI 심사 업무 정책의 수정 가능한 필드를 변경합니다.
     *
     * @param policyName 정책명
     * @param corporateTypeCode 법인 유형 코드
     * @param autoApproveYn 자동 승인 사용 여부
     * @param autoApproveThreshold 자동 승인 기준
     * @param manualReviewThreshold 수동 심사 기준
     * @param supplementThreshold 보완요청 후보 기준
     * @param adminId 수정 관리자 ID
     */
    public void update(
            String policyName,
            String corporateTypeCode,
            String autoApproveYn,
            BigDecimal autoApproveThreshold,
            BigDecimal manualReviewThreshold,
            BigDecimal supplementThreshold,
            Long adminId
    ) {
        if (policyName != null) {
            this.policyName = policyName;
        }
        if (corporateTypeCode != null) {
            this.corporateTypeCode = corporateTypeCode;
        }
        if (autoApproveYn != null) {
            this.autoApproveYn = autoApproveYn;
        }
        if (autoApproveThreshold != null) {
            this.autoApproveThreshold = autoApproveThreshold;
        }
        if (manualReviewThreshold != null) {
            this.manualReviewThreshold = manualReviewThreshold;
        }
        if (supplementThreshold != null) {
            this.supplementThreshold = supplementThreshold;
        }
        this.updatedByAdminId = adminId;
    }

    /**
     * AI 심사 업무 정책 사용 여부를 변경합니다.
     *
     * @param enabledYn 사용 여부
     * @param adminId 수정 관리자 ID
     */
    public void changeEnabled(String enabledYn, Long adminId) {
        this.enabledYn = enabledYn;
        this.updatedByAdminId = adminId;
    }
}
