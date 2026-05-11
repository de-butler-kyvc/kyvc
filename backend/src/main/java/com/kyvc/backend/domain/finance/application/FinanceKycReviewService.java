package com.kyvc.backend.domain.finance.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.document.application.RequiredDocumentPolicyProvider;
import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorage;
import com.kyvc.backend.domain.finance.dto.FinanceKycResultResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycSubmitResponse;
import com.kyvc.backend.domain.finance.repository.FinanceKycApplicationRepository;
import com.kyvc.backend.domain.finance.repository.FinanceKycDocumentRepository;
import com.kyvc.backend.domain.finance.repository.FinanceKycQrRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.security.CustomUserDetails;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

// 금융사 방문 KYC 심사 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class FinanceKycReviewService {

    private static final String CORE_ASSESSMENT_NORMAL = "NORMAL"; // Core 정상 판정
    private static final String CORE_ASSESSMENT_SUPPLEMENT_REQUIRED = "SUPPLEMENT_REQUIRED"; // Core 보완 필요 판정
    private static final String CORE_ASSESSMENT_MANUAL_REVIEW_REQUIRED = "MANUAL_REVIEW_REQUIRED"; // Core 수동심사 필요 판정
    private static final String CORE_ASSESSMENT_REJECTED = "REJECTED"; // Core 반려 판정
    private static final String AI_REVIEW_MANUAL_REASON = "AI 심사 결과 수동심사 필요"; // 수동심사 전환 사유
    private static final String AI_REVIEW_FAILED_MANUAL_REASON = "AI 심사 실패로 수동심사 전환"; // AI 실패 전환 사유
    private static final BigDecimal DEFAULT_CONFIDENCE_SCORE = BigDecimal.ZERO; // 기본 신뢰도 점수

    private final FinanceContextService financeContextService;
    private final FinanceKycApplicationRepository financeKycApplicationRepository;
    private final FinanceKycDocumentRepository financeKycDocumentRepository;
    private final FinanceKycQrRepository financeKycQrRepository;
    private final CorporateRepository corporateRepository;
    private final RequiredDocumentPolicyProvider requiredDocumentPolicyProvider;
    private final DocumentStorage documentStorage;
    private final CoreRequestService coreRequestService;
    private final CoreAdapter coreAdapter;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    // 금융사 방문 KYC 제출 및 AI 심사
    public FinanceKycSubmitResponse submit(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long kycId // KYC 신청 ID
    ) {
        validateKycId(kycId);
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        KycApplication kycApplication = findAccessibleFinanceKyc(context, kycId);
        if (!kycApplication.isDraft()) {
            throw new ApiException(ErrorCode.FINANCE_KYC_SUBMIT_NOT_ALLOWED);
        }

        Corporate corporate = corporateRepository.findById(kycApplication.getCorporateId())
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        List<KycDocument> documents = financeKycDocumentRepository.findByKycId(kycId); // 제출 문서 목록
        validateRequiredInfo(kycApplication, corporate);
        validateRequiredDocuments(kycApplication, documents);

        LocalDateTime submittedAt = LocalDateTime.now(); // 제출 일시
        String coreRequestId = null; // Core 요청 ID

        try {
            CoreRequest coreRequest = coreRequestService.createAiReviewRequest(kycId, null);
            coreRequestId = coreRequest.getCoreRequestId();
            kycApplication.startAiReview(submittedAt);

            CoreAiReviewRequest coreRequestPayload = buildAiReviewRequest(
                    coreRequestId,
                    kycApplication,
                    corporate,
                    documents,
                    submittedAt
            );
            coreRequestService.updateRequestPayloadJson(coreRequestId, toJson(coreRequestPayload));
            coreRequestService.markRunning(coreRequestId);
            CoreAiReviewResponse coreResponse = coreAdapter.requestAiReview(coreRequestPayload);
            coreRequestService.markSuccess(coreRequestId, toJson(coreResponse));

            applyAiReviewResult(kycApplication, coreResponse);
            KycApplication savedApplication = financeKycApplicationRepository.save(kycApplication);
            saveAudit(
                    context.userId(),
                    savedApplication.getKycId(),
                    "FINANCE_KYC_SUBMIT",
                    "금융사 방문 KYC 제출 및 AI 심사 결과 반영"
            );
            return toSubmitResponse(savedApplication);
        } catch (ApiException exception) {
            if (coreRequestId == null) {
                throw exception;
            }
            markCoreRequestFailure(coreRequestId, exception);
            kycApplication.failAiReviewAsManualReview(AI_REVIEW_FAILED_MANUAL_REASON);
            KycApplication savedApplication = financeKycApplicationRepository.save(kycApplication);
            saveAudit(
                    context.userId(),
                    savedApplication.getKycId(),
                    "FINANCE_KYC_AI_REVIEW_FAILED",
                    "금융사 방문 KYC AI 심사 실패 후 수동심사 전환"
            );
            return toSubmitResponse(savedApplication);
        }
    }

    // 금융사 방문 KYC 결과 조회
    @Transactional(readOnly = true)
    public FinanceKycResultResponse getResult(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long kycId // KYC 신청 ID
    ) {
        validateKycId(kycId);
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        KycApplication kycApplication = findAccessibleFinanceKyc(context, kycId);
        if (kycApplication.getAiReviewStatus() == null && kycApplication.getAiReviewResult() == null) {
            throw new ApiException(ErrorCode.FINANCE_KYC_RESULT_NOT_FOUND);
        }

        Credential credential = financeKycQrRepository.findLatestByKycId(kycId).orElse(null); // 최신 Credential
        return new FinanceKycResultResponse(
                kycApplication.getKycId(),
                enumName(kycApplication.getKycStatus()),
                enumName(kycApplication.getAiReviewStatus()),
                resolveResultSummary(kycApplication),
                toYn(isManualReviewRequired(kycApplication)),
                toYn(KyvcEnums.KycStatus.NEED_SUPPLEMENT == kycApplication.getKycStatus()),
                toYn(kycApplication.isCredentialIssuable()),
                credential == null ? null : enumName(credential.getCredentialStatus())
        );
    }

    // Core AI 심사 요청 생성
    private CoreAiReviewRequest buildAiReviewRequest(
            String coreRequestId, // Core 요청 ID
            KycApplication kycApplication, // KYC 신청
            Corporate corporate, // 법인 정보
            List<KycDocument> documents, // 제출 문서 목록
            LocalDateTime requestedAt // 요청 일시
    ) {
        return new CoreAiReviewRequest(
                coreRequestId,
                kycApplication.getKycId(),
                corporate.getCorporateId(),
                corporate.getBusinessRegistrationNo(),
                corporate.getCorporateRegistrationNo(),
                corporate.getCorporateName(),
                corporate.getRepresentativeName(),
                corporate.getRepresentativePhone(),
                corporate.getRepresentativeEmail(),
                corporate.getAgentName(),
                kycApplication.getCorporateTypeCode(),
                buildAiReviewDocuments(documents),
                requestedAt
        );
    }

    // Core AI 심사 문서 목록 생성
    private List<CoreAiReviewRequest.CoreAiReviewDocumentRequest> buildAiReviewDocuments(
            List<KycDocument> documents // 제출 문서 목록
    ) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
                .filter(document -> KyvcEnums.DocumentUploadStatus.UPLOADED == document.getUploadStatus())
                .map(document -> new CoreAiReviewRequest.CoreAiReviewDocumentRequest(
                        document.getDocumentId(),
                        document.getDocumentTypeCode(),
                        document.getDocumentHash(),
                        document.getFileName(),
                        document.getMimeType(),
                        document.getFilePath(),
                        loadDocumentContentBase64(document),
                        document.getFileSize()
                ))
                .toList();
    }

    // Core 전송용 문서 Base64 생성
    private String loadDocumentContentBase64(
            KycDocument document // 심사 대상 문서
    ) {
        try (InputStream inputStream = documentStorage.load(document.getFilePath()).resource().getInputStream()) {
            return Base64.getEncoder().encodeToString(inputStream.readAllBytes());
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_FILE_NOT_FOUND, exception);
        }
    }

    // AI 심사 결과 반영
    private void applyAiReviewResult(
            KycApplication kycApplication, // KYC 신청
            CoreAiReviewResponse coreResponse // Core AI 심사 응답
    ) {
        KyvcEnums.AiReviewStatus aiReviewStatus = resolveAiReviewStatus(coreResponse); // AI 심사 상태
        String detailJson = toJson(coreResponse); // AI 심사 상세 JSON
        BigDecimal confidenceScore = coreResponse == null || coreResponse.confidenceScore() == null
                ? DEFAULT_CONFIDENCE_SCORE
                : coreResponse.confidenceScore(); // AI 신뢰도 점수
        String summary = coreResponse == null ? null : coreResponse.message(); // AI 심사 요약
        String assessmentStatus = normalizeAssessmentStatus(coreResponse); // Core 판정 상태

        if (KyvcEnums.AiReviewStatus.FAILED == aiReviewStatus) {
            kycApplication.failAiReviewAsManualReview(AI_REVIEW_FAILED_MANUAL_REASON);
            return;
        }
        if (CORE_ASSESSMENT_NORMAL.equals(assessmentStatus)) {
            kycApplication.completeAiReviewAsApproved(confidenceScore, summary, detailJson, LocalDateTime.now());
            return;
        }
        if (CORE_ASSESSMENT_SUPPLEMENT_REQUIRED.equals(assessmentStatus)) {
            kycApplication.completeAiReviewAsNeedSupplement(confidenceScore, summary, detailJson, summary);
            return;
        }
        if (CORE_ASSESSMENT_REJECTED.equals(assessmentStatus)) {
            kycApplication.completeAiReviewAsRejected(confidenceScore, summary, detailJson, summary, LocalDateTime.now());
            return;
        }
        if (CORE_ASSESSMENT_MANUAL_REVIEW_REQUIRED.equals(assessmentStatus)
                || KyvcEnums.AiReviewStatus.LOW_CONFIDENCE == aiReviewStatus) {
            kycApplication.completeAiReviewAsLowConfidenceManualReview(
                    confidenceScore,
                    summary,
                    detailJson,
                    AI_REVIEW_MANUAL_REASON
            );
            return;
        }
        kycApplication.completeAiReviewAsManualReview(confidenceScore, summary, detailJson, AI_REVIEW_MANUAL_REASON);
    }

    // 접근 가능한 금융사 방문 KYC 조회
    private KycApplication findAccessibleFinanceKyc(
            FinanceContextService.FinanceContext context, // 금융사 직원 컨텍스트
            Long kycId // KYC 신청 ID
    ) {
        KycApplication kycApplication = financeKycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.FINANCE_KYC_NOT_FOUND));
        if (!kycApplication.isFinanceVisit()) {
            throw new ApiException(ErrorCode.FINANCE_KYC_ACCESS_DENIED);
        }
        if (!kycApplication.isFinanceVisitByStaff(context.userId())) {
            throw new ApiException(ErrorCode.FINANCE_KYC_ACCESS_DENIED);
        }
        return kycApplication;
    }

    // 필수 법인정보 검증
    private void validateRequiredInfo(
            KycApplication kycApplication, // KYC 신청
            Corporate corporate // 법인 정보
    ) {
        if (!StringUtils.hasText(corporate.getCorporateName())
                || !StringUtils.hasText(corporate.getBusinessRegistrationNo())
                || !StringUtils.hasText(corporate.getRepresentativeName())
                || !StringUtils.hasText(corporate.getAddress())
                || !StringUtils.hasText(kycApplication.getCorporateTypeCode())) {
            throw new ApiException(ErrorCode.FINANCE_KYC_REQUIRED_INFO_MISSING);
        }
    }

    // 필수 문서 검증
    private void validateRequiredDocuments(
            KycApplication kycApplication, // KYC 신청
            List<KycDocument> documents // 제출 문서 목록
    ) {
        Set<String> uploadedTypeCodes = documents == null ? Set.of() : documents.stream()
                .filter(document -> KyvcEnums.DocumentUploadStatus.UPLOADED == document.getUploadStatus())
                .map(KycDocument::getDocumentTypeCode)
                .collect(Collectors.toSet()); // 업로드 완료 문서 유형 목록
        boolean missingRequiredDocument = requiredDocumentPolicyProvider
                .getRequiredDocuments(kycApplication.getCorporateTypeCode())
                .stream()
                .filter(RequiredDocumentPolicyProvider.RequiredDocumentPolicy::required)
                .anyMatch(policy -> !uploadedTypeCodes.contains(policy.documentTypeCode()));
        if (missingRequiredDocument) {
            throw new ApiException(ErrorCode.FINANCE_KYC_REQUIRED_DOCUMENT_MISSING);
        }
    }

    // AI 심사 상태 결정
    private KyvcEnums.AiReviewStatus resolveAiReviewStatus(
            CoreAiReviewResponse coreResponse // Core AI 심사 응답
    ) {
        if (coreResponse == null || !StringUtils.hasText(coreResponse.status())) {
            return KyvcEnums.AiReviewStatus.FAILED;
        }
        try {
            return KyvcEnums.AiReviewStatus.valueOf(coreResponse.status().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return KyvcEnums.AiReviewStatus.FAILED;
        }
    }

    // Core 판정 상태 정규화
    private String normalizeAssessmentStatus(
            CoreAiReviewResponse coreResponse // Core AI 심사 응답
    ) {
        if (coreResponse == null || !StringUtils.hasText(coreResponse.assessmentStatus())) {
            return null;
        }
        return coreResponse.assessmentStatus().trim().toUpperCase(Locale.ROOT);
    }

    // Core 요청 실패 상태 반영
    private void markCoreRequestFailure(
            String coreRequestId, // Core 요청 ID
            ApiException exception // Core 호출 예외
    ) {
        if (ErrorCode.CORE_API_TIMEOUT == exception.getErrorCode()) {
            coreRequestService.markTimeout(coreRequestId, exception.getMessage());
            return;
        }
        coreRequestService.markFailed(coreRequestId, exception.getMessage());
    }

    // 제출 응답 변환
    private FinanceKycSubmitResponse toSubmitResponse(
            KycApplication kycApplication // KYC 신청
    ) {
        return new FinanceKycSubmitResponse(
                kycApplication.getKycId(),
                enumName(kycApplication.getKycStatus()),
                enumName(kycApplication.getAiReviewStatus()),
                toYn(isManualReviewRequired(kycApplication))
        );
    }

    // 결과 요약 결정
    private String resolveResultSummary(
            KycApplication kycApplication // KYC 신청
    ) {
        if (StringUtils.hasText(kycApplication.getAiReviewSummary())) {
            return kycApplication.getAiReviewSummary();
        }
        if (StringUtils.hasText(kycApplication.getManualReviewReason())) {
            return kycApplication.getManualReviewReason();
        }
        return kycApplication.getRejectReason();
    }

    // 수동심사 필요 여부
    private boolean isManualReviewRequired(
            KycApplication kycApplication // KYC 신청
    ) {
        return KyvcEnums.KycStatus.MANUAL_REVIEW == kycApplication.getKycStatus()
                || KyvcEnums.AiReviewResult.NEED_MANUAL_REVIEW == kycApplication.getAiReviewResult()
                || StringUtils.hasText(kycApplication.getManualReviewReason());
    }

    // 감사로그 저장
    private void saveAudit(
            Long userId, // 사용자 ID
            Long kycId, // KYC 신청 ID
            String actionType, // 작업 유형
            String summary // 요청 요약
    ) {
        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.FINANCE.name(),
                userId,
                actionType,
                KyvcEnums.AuditTargetType.KYC_APPLICATION.name(),
                kycId,
                summary,
                null
        ));
    }

    // JSON 직렬화
    private String toJson(
            Object value // 직렬화 대상
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    // Y/N 변환
    private String toYn(
            boolean value // 변환 대상 여부
    ) {
        return value ? KyvcEnums.Yn.Y.name() : KyvcEnums.Yn.N.name();
    }

    // enum 이름 변환
    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    // KYC ID 검증
    private void validateKycId(
            Long kycId // KYC 신청 ID
    ) {
        if (kycId == null || kycId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }
}
