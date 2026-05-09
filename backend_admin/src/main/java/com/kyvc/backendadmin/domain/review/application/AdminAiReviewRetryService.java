package com.kyvc.backendadmin.domain.review.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.core.application.CoreRequestService;
import com.kyvc.backendadmin.domain.document.domain.KycDocument;
import com.kyvc.backendadmin.domain.document.repository.KycDocumentRepository;
import com.kyvc.backendadmin.domain.kyc.domain.KycApplication;
import com.kyvc.backendadmin.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backendadmin.domain.review.domain.KycReviewHistory;
import com.kyvc.backendadmin.domain.review.dto.AiReviewRetryRequest;
import com.kyvc.backendadmin.domain.review.dto.AiReviewRetryResponse;
import com.kyvc.backendadmin.domain.review.repository.AdminReviewRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Set;

/**
 * Backend Admin AI 재심사 요청 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminAiReviewRetryService {

    private static final String AUDIT_ACTION_AI_REVIEW_RETRY_REQUESTED = "AI_REVIEW_RETRY_REQUESTED";

    private static final Set<KyvcEnums.KycStatus> RETRYABLE_KYC_STATUSES = EnumSet.of(
            KyvcEnums.KycStatus.SUBMITTED,
            KyvcEnums.KycStatus.MANUAL_REVIEW,
            KyvcEnums.KycStatus.NEED_SUPPLEMENT
    );

    private final KycApplicationRepository kycApplicationRepository;
    private final CoreRequestService coreRequestService;
    private final AdminReviewRepository adminReviewRepository;
    private final AuditLogWriter auditLogWriter;
    private final KycDocumentRepository kycDocumentRepository;

    /**
     * KYC 신청 건에 대해 AI 재심사 요청을 생성합니다.
     *
     * @param kycId KYC 신청 ID
     * @param request AI 재심사 요청 정보
     * @return AI 재심사 요청 생성 결과
     */
    @Transactional
    public AiReviewRetryResponse retry(Long kycId, AiReviewRetryRequest request) {
        KycApplication application = findKycApplication(kycId);
        validateReason(request);
        validateRetryableStatus(application.getKycStatusCode());
        validateDocumentsBelongToKyc(kycId, request.documentIds());

        // core_requests row 생성: 실제 AI 심사는 Core에서 수행하므로 요청 대기 row만 생성한다.
        String coreRequestId = coreRequestService.createAiReviewRequest(kycId, request.reason(), request.documentIds());

        // kyc_applications.ai_review_status 변경: Core가 처리할 수 있도록 AI 심사 상태를 대기 상태로 되돌린다.
        int updatedRows = kycApplicationRepository.updateAiReviewStatus(kycId, KyvcEnums.AiReviewStatus.QUEUED);
        if (updatedRows != 1) {
            throw new ApiException(ErrorCode.KYC_NOT_FOUND);
        }

        Long adminId = SecurityUtil.getCurrentAdminId();
        // kyc_review_histories 기록: 재심사 요청은 KYC 상태 전이가 아니라 이벤트 이력이므로 before/after는 현재 상태로 기록한다.
        adminReviewRepository.saveReviewHistory(KycReviewHistory.create(
                kycId,
                adminId,
                KyvcEnums.ReviewActionType.REQUEST_AI_REVIEW,
                application.getKycStatusCode(),
                application.getKycStatusCode(),
                request.reason()
        ));

        // audit_logs 기록: 관리자에 의한 AI 재심사 요청과 생성된 Core 요청 ID를 감사 로그에 남긴다.
        auditLogWriter.write(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                AUDIT_ACTION_AI_REVIEW_RETRY_REQUESTED,
                KyvcEnums.AuditTargetType.KYC_APPLICATION,
                kycId,
                "AI 재심사 요청 생성. coreRequestId=%s".formatted(coreRequestId),
                application.getAiReviewStatusCode() == null ? null : application.getAiReviewStatusCode().name(),
                KyvcEnums.AiReviewStatus.QUEUED.name()
        );

        return new AiReviewRetryResponse(
                kycId,
                coreRequestId,
                KyvcEnums.CoreRequestType.AI_REVIEW.name(),
                KyvcEnums.CoreRequestStatus.QUEUED.name()
        );
    }

    private KycApplication findKycApplication(Long kycId) {
        return kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
    }

    private void validateReason(AiReviewRetryRequest request) {
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "reason은 필수입니다.");
        }
    }

    private void validateRetryableStatus(KyvcEnums.KycStatus kycStatus) {
        // 재심사 가능 상태 검증: 제출 완료, 수동 심사, 보완 필요 상태에서만 AI 재심사를 요청할 수 있다.
        if (!RETRYABLE_KYC_STATUSES.contains(kycStatus)) {
            throw new ApiException(ErrorCode.INVALID_AI_REVIEW_RETRY_STATUS);
        }
    }

    private void validateDocumentsBelongToKyc(Long kycId, java.util.List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        for (Long documentId : documentIds) {
            KycDocument document = kycDocumentRepository.findById(documentId)
                    .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
            if (!kycId.equals(document.getKycId())) {
                throw new ApiException(ErrorCode.DOCUMENT_ACCESS_DENIED);
            }
        }
    }
}
