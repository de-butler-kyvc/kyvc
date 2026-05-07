package com.kyvc.backendadmin.domain.review.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.review.domain.AiReviewPolicy;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicyCreateRequest;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicyEnabledUpdateRequest;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicyResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicyUpdateRequest;
import com.kyvc.backendadmin.domain.review.repository.AiReviewPolicyQueryRepository;
import com.kyvc.backendadmin.domain.review.repository.AiReviewPolicyRepository;
import com.kyvc.backendadmin.global.commoncode.application.CommonCodeValidator;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * AI 심사 업무 정책 등록/수정 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AiReviewPolicyService {

    private static final String CORPORATE_TYPE_CODE_GROUP = "CORPORATE_TYPE";
    private static final BigDecimal MIN_THRESHOLD = BigDecimal.ZERO;
    private static final BigDecimal MAX_THRESHOLD = BigDecimal.ONE;

    private final AiReviewPolicyRepository aiReviewPolicyRepository;
    private final AiReviewPolicyQueryRepository aiReviewPolicyQueryRepository;
    private final CommonCodeValidator commonCodeValidator;
    private final AuditLogWriter auditLogWriter;

    /**
     * AI 심사 업무 정책을 등록합니다.
     *
     * @param request AI 심사 정책 등록 요청
     * @return 등록된 AI 심사 정책 상세 응답
     */
    @Transactional
    public AiReviewPolicyResponse create(AiReviewPolicyCreateRequest request) {
        validateCreateRequest(request);
        Long adminId = SecurityUtil.getCurrentAdminId();
        AiReviewPolicy policy = AiReviewPolicy.create(
                request.policyName(),
                request.corporateTypeCode(),
                request.autoApproveYn(),
                request.autoApproveThreshold(),
                request.manualReviewThreshold(),
                request.supplementThreshold(),
                adminId
        );
        aiReviewPolicyRepository.save(policy);

        // audit_logs 기록: AI 심사 정책 등록 이력을 관리자와 정책 ID 기준으로 남긴다.
        auditLogWriter.write(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                "AI_REVIEW_POLICY_CREATED",
                KyvcEnums.AuditTargetType.AI_REVIEW_POLICY,
                policy.getAiPolicyId(),
                "AI 심사 정책 등록. policyName=%s".formatted(policy.getPolicyName()),
                null,
                request.description()
        );
        return getSavedDetail(policy.getAiPolicyId());
    }

    /**
     * AI 심사 업무 정책을 수정합니다.
     *
     * @param aiPolicyId AI 심사 정책 ID
     * @param request AI 심사 정책 수정 요청
     * @return 수정된 AI 심사 정책 상세 응답
     */
    @Transactional
    public AiReviewPolicyResponse update(Long aiPolicyId, AiReviewPolicyUpdateRequest request) {
        AiReviewPolicy policy = findPolicy(aiPolicyId);
        validateUpdateRequest(request, policy);
        Long adminId = SecurityUtil.getCurrentAdminId();
        String beforeValue = summarize(policy);
        policy.update(
                normalizeBlankToNull(request.policyName()),
                normalizeBlankToNull(request.corporateTypeCode()),
                normalizeBlankToNull(request.autoApproveYn()),
                request.autoApproveThreshold(),
                request.manualReviewThreshold(),
                request.supplementThreshold(),
                adminId
        );

        // audit_logs 기록: 수정 전후 핵심 기준값을 감사 로그 요약에 남긴다.
        auditLogWriter.write(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                "AI_REVIEW_POLICY_UPDATED",
                KyvcEnums.AuditTargetType.AI_REVIEW_POLICY,
                aiPolicyId,
                "AI 심사 정책 수정. description=%s".formatted(request.description()),
                beforeValue,
                summarize(policy)
        );
        return getSavedDetail(aiPolicyId);
    }

    /**
     * AI 심사 업무 정책 사용 여부를 변경합니다.
     *
     * @param aiPolicyId AI 심사 정책 ID
     * @param request 사용 여부 변경 요청
     * @return 변경된 AI 심사 정책 상세 응답
     */
    @Transactional
    public AiReviewPolicyResponse changeEnabled(Long aiPolicyId, AiReviewPolicyEnabledUpdateRequest request) {
        AiReviewPolicy policy = findPolicy(aiPolicyId);
        validateEnabledYn(request.enabledYn());
        Long adminId = SecurityUtil.getCurrentAdminId();
        String beforeEnabledYn = policy.getEnabledYn();
        policy.changeEnabled(request.enabledYn(), adminId);

        // audit_logs 기록: 정책 사용 여부 변경 전후 값을 남긴다.
        auditLogWriter.write(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                "AI_REVIEW_POLICY_ENABLED_CHANGED",
                KyvcEnums.AuditTargetType.AI_REVIEW_POLICY,
                aiPolicyId,
                "AI 심사 정책 사용 여부 변경",
                beforeEnabledYn,
                request.enabledYn()
        );
        return getSavedDetail(aiPolicyId);
    }

    private void validateCreateRequest(AiReviewPolicyCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.policyName())) {
            throw new ApiException(ErrorCode.INVALID_AI_REVIEW_POLICY, "policyName은 필수입니다.");
        }
        commonCodeValidator.validateEnabledCode(CORPORATE_TYPE_CODE_GROUP, request.corporateTypeCode());
        validateEnabledYn(request.autoApproveYn());
        validateThresholds(
                request.autoApproveThreshold(),
                request.manualReviewThreshold(),
                request.supplementThreshold()
        );
    }

    private void validateUpdateRequest(AiReviewPolicyUpdateRequest request, AiReviewPolicy currentPolicy) {
        if (request == null) {
            throw new ApiException(ErrorCode.INVALID_AI_REVIEW_POLICY);
        }
        if (StringUtils.hasText(request.corporateTypeCode())) {
            commonCodeValidator.validateEnabledCode(CORPORATE_TYPE_CODE_GROUP, request.corporateTypeCode());
        }
        if (StringUtils.hasText(request.autoApproveYn())) {
            validateEnabledYn(request.autoApproveYn());
        }
        validateThresholds(
                request.autoApproveThreshold() == null ? currentPolicy.getAutoApproveThreshold() : request.autoApproveThreshold(),
                request.manualReviewThreshold() == null ? currentPolicy.getManualReviewThreshold() : request.manualReviewThreshold(),
                request.supplementThreshold() == null ? currentPolicy.getSupplementThreshold() : request.supplementThreshold()
        );
    }

    private void validateThresholds(
            BigDecimal autoApproveThreshold,
            BigDecimal manualReviewThreshold,
            BigDecimal supplementThreshold
    ) {
        validateThreshold(autoApproveThreshold, "autoApproveThreshold");
        validateThreshold(manualReviewThreshold, "manualReviewThreshold");
        validateThreshold(supplementThreshold, "supplementThreshold");
        // autoApproveThreshold와 manualReviewThreshold 관계 검증: 자동 승인 기준은 수동 심사 전환 기준보다 낮을 수 없다.
        if (autoApproveThreshold.compareTo(manualReviewThreshold) < 0) {
            throw new ApiException(ErrorCode.INVALID_AI_REVIEW_POLICY, "autoApproveThreshold는 manualReviewThreshold 이상이어야 합니다.");
        }
    }

    private void validateThreshold(BigDecimal threshold, String fieldName) {
        // threshold 값은 AI 신뢰도 기준이므로 0.0 이상 1.0 이하만 허용한다.
        if (threshold == null
                || threshold.compareTo(MIN_THRESHOLD) < 0
                || threshold.compareTo(MAX_THRESHOLD) > 0) {
            throw new ApiException(ErrorCode.INVALID_AI_REVIEW_POLICY, fieldName + "는 0.0 이상 1.0 이하이어야 합니다.");
        }
    }

    private void validateEnabledYn(String yn) {
        // enabledYn Y/N 검증: 사용 여부와 자동 승인 여부는 Y 또는 N만 허용한다.
        if (!"Y".equals(yn) && !"N".equals(yn)) {
            throw new ApiException(ErrorCode.INVALID_CODE_VALUE, "Y 또는 N만 사용할 수 있습니다.");
        }
    }

    private AiReviewPolicy findPolicy(Long aiPolicyId) {
        return aiReviewPolicyRepository.findById(aiPolicyId)
                .orElseThrow(() -> new ApiException(ErrorCode.AI_REVIEW_POLICY_NOT_FOUND));
    }

    private AiReviewPolicyResponse getSavedDetail(Long aiPolicyId) {
        return aiReviewPolicyQueryRepository.findDetailById(aiPolicyId)
                .orElseThrow(() -> new ApiException(ErrorCode.AI_REVIEW_POLICY_NOT_FOUND));
    }

    private String normalizeBlankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String summarize(AiReviewPolicy policy) {
        return "policyName=%s, corporateType=%s, autoApproveYn=%s, autoApproveThreshold=%s, manualReviewThreshold=%s, supplementThreshold=%s, enabledYn=%s"
                .formatted(
                        policy.getPolicyName(),
                        policy.getCorporateTypeCode(),
                        policy.getAutoApproveYn(),
                        policy.getAutoApproveThreshold(),
                        policy.getManualReviewThreshold(),
                        policy.getSupplementThreshold(),
                        policy.getEnabledYn()
                );
    }
}
