package com.kyvc.backend.domain.kyc.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.config.CoreInternalProperties;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.document.application.RequiredDocumentPolicyProvider;
import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.dto.KycDocumentResponse;
import com.kyvc.backend.domain.document.dto.RequiredDocumentResponse;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.dto.KycApplicationSummaryResponse;
import com.kyvc.backend.domain.kyc.dto.KycMissingItemResponse;
import com.kyvc.backend.domain.kyc.dto.KycSubmitResponse;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// KYC 제출 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class KycSubmissionService {

    private static final String CORPORATE_NAME_REQUIRED = "CORPORATE_NAME_REQUIRED"; // 법인명 누락 코드
    private static final String BUSINESS_REGISTRATION_NO_REQUIRED = "BUSINESS_REGISTRATION_NO_REQUIRED"; // 사업자등록번호 누락 코드
    private static final String REPRESENTATIVE_REQUIRED = "REPRESENTATIVE_REQUIRED"; // 대표자 정보 누락 코드
    private static final String CORPORATE_TYPE_REQUIRED = "CORPORATE_TYPE_REQUIRED"; // 법인 유형 누락 코드
    private static final String DOCUMENT_STORE_OPTION_REQUIRED = "DOCUMENT_STORE_OPTION_REQUIRED"; // 원본서류 저장 옵션 누락 코드
    private static final String DOCUMENT_REQUIRED = "DOCUMENT_REQUIRED"; // 필수서류 누락 코드
    private static final String AI_REVIEW_REQUESTED_MESSAGE = "KYC 신청이 제출되었고 AI 심사가 요청되었습니다.";

    private final KycApplicationRepository kycApplicationRepository;
    private final CorporateRepository corporateRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final RequiredDocumentPolicyProvider requiredDocumentPolicyProvider;
    private final CoreRequestService coreRequestService;
    private final CoreAdapter coreAdapter;
    private final CoreInternalProperties coreInternalProperties;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    // KYC 제출 요약 조회
    @Transactional(readOnly = true)
    public KycApplicationSummaryResponse getSummary(
            Long userId, // 사용자 ID
            Long kycId // KYC 요청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        return buildSummary(userId, findOwnedKyc(userId, kycId));
    }

    // KYC 제출
    public KycSubmitResponse submit(
            Long userId, // 사용자 ID
            Long kycId // KYC 요청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);

        KycApplication kycApplication = findOwnedKyc(userId, kycId); // 사용자 소유 KYC
        if (!kycApplication.isDraft()) {
            throw new ApiException(ErrorCode.KYC_INVALID_STATUS);
        }

        KycApplicationSummaryResponse summary = buildSummary(userId, kycApplication); // 제출 전 요약 정보
        validateSubmittable(summary);

        LocalDateTime submittedAt = LocalDateTime.now(); // 제출 일시
        String coreRequestId = null; // Core 요청 ID

        logEventLogger.info(
                "kyc.submit.started",
                "KYC submit started",
                createSubmitLogFields(kycApplication, null)
        );

        try {
            CoreRequest coreRequest = coreRequestService.createAiReviewRequest(
                    kycApplication.getKycId(),
                    null
            );
            coreRequestId = coreRequest.getCoreRequestId();

            CoreAiReviewRequest coreAiReviewRequest = buildAiReviewRequest(
                    coreRequestId,
                    summary,
                    submittedAt,
                    buildAiReviewCallbackUrl(coreRequestId)
            ); // Core AI 심사 API 미연동 상태의 Stub 요청

            String requestPayloadJson = toJson(coreAiReviewRequest);
            coreRequestService.updateRequestPayloadJson(coreRequestId, requestPayloadJson);

            logEventLogger.info(
                    "core.call.started",
                    "Core AI review call started",
                    createSubmitLogFields(kycApplication, coreRequestId)
            );
            CoreAiReviewResponse coreAiReviewResponse = coreAdapter.requestAiReview(coreAiReviewRequest);
            coreRequestService.markRequested(coreRequestId, toJson(coreAiReviewResponse));
            logEventLogger.info(
                    "core.call.completed",
                    "Core AI review call completed",
                    createSubmitLogFields(kycApplication, coreRequestId)
            );

            logEventLogger.info(
                    "kyc.submit.ai-review-requested",
                    "KYC submit AI review requested",
                    createSubmitLogFields(kycApplication, coreRequestId)
            );

            kycApplication.startAiReview(submittedAt);
            KycApplication savedApplication = kycApplicationRepository.save(kycApplication); // 저장 완료 KYC

            logEventLogger.info(
                    "kyc.submit.completed",
                    "KYC submit completed",
                    createSubmitLogFields(savedApplication, coreRequestId)
            );

            return new KycSubmitResponse(
                    savedApplication.getKycId(),
                    savedApplication.getKycStatus().name(),
                    savedApplication.getSubmittedAt(),
                    true,
                    AI_REVIEW_REQUESTED_MESSAGE
            );
        } catch (ApiException exception) {
            if (coreRequestId != null) {
                logEventLogger.warn(
                        "core.call.failed",
                        exception.getMessage(),
                        createSubmitLogFields(kycApplication, coreRequestId)
                );
            }
            logEventLogger.warn(
                    "kyc.submit.failed",
                    exception.getMessage(),
                    createSubmitLogFields(kycApplication, coreRequestId)
            );
            throw exception;
        }
    }

    // 제출 전 요약 생성
    private KycApplicationSummaryResponse buildSummary(
            Long userId, // 사용자 ID
            KycApplication kycApplication // KYC 요청 정보
    ) {
        Corporate corporate = findOwnedCorporate(userId, kycApplication.getCorporateId()); // 소유 법인 정보
        List<KycDocument> documents = kycDocumentRepository.findByKycId(kycApplication.getKycId()); // 업로드 문서 목록
        List<KycDocumentResponse> documentResponses = documents.stream()
                .map(this::toDocumentResponse)
                .toList();
        List<RequiredDocumentResponse> requiredDocuments = buildRequiredDocuments(kycApplication, documents); // 필수서류 충족 여부 목록
        List<KycMissingItemResponse> missingItems = buildMissingItems(corporate, kycApplication, documents); // 누락 항목 목록
        boolean submittable = kycApplication.isDraft() && isSubmittable(missingItems); // 제출 가능 여부

        return new KycApplicationSummaryResponse(
                kycApplication.getKycId(),
                kycApplication.getKycStatus().name(),
                corporate.getCorporateId(),
                corporate.getCorporateName(),
                corporate.getBusinessRegistrationNo(),
                corporate.getCorporateRegistrationNo(),
                corporate.getRepresentativeName(),
                corporate.getRepresentativePhone(),
                corporate.getRepresentativeEmail(),
                corporate.getAgentName(),
                corporate.getAgentPhone(),
                corporate.getAgentEmail(),
                corporate.getAgentAuthorityScope(),
                kycApplication.getCorporateTypeCode(),
                enumName(kycApplication.getOriginalDocumentStoreOption()),
                documentResponses,
                requiredDocuments,
                submittable,
                missingItems,
                kycApplication.getCreatedAt(),
                kycApplication.getUpdatedAt(),
                kycApplication.getSubmittedAt()
        );
    }

    // 필수서류 충족 여부 목록 생성
    private List<RequiredDocumentResponse> buildRequiredDocuments(
            KycApplication kycApplication, // KYC 요청 정보
            List<KycDocument> documents // 업로드 문서 목록
    ) {
        Set<String> uploadedDocumentTypeCodes = getUploadedDocumentTypeCodes(documents); // 업로드 문서 유형 코드 목록
        return requiredDocumentPolicyProvider.getRequiredDocuments(kycApplication.getCorporateTypeCode()).stream()
                .map(policy -> new RequiredDocumentResponse(
                        policy.documentTypeCode(),
                        policy.documentTypeName(),
                        policy.required(),
                        uploadedDocumentTypeCodes.contains(policy.documentTypeCode()),
                        policy.description(),
                        policy.allowedExtensions(),
                        policy.maxFileSizeMb()
                ))
                .toList();
    }

    // 누락 항목 목록 생성
    private List<KycMissingItemResponse> buildMissingItems(
            Corporate corporate, // 법인 정보
            KycApplication kycApplication, // KYC 요청 정보
            List<KycDocument> documents // 업로드 문서 목록
    ) {
        Set<KycMissingItemResponse> missingItems = new LinkedHashSet<>(); // 누락 항목 목록

        if (!StringUtils.hasText(corporate.getCorporateName())) {
            missingItems.add(new KycMissingItemResponse(
                    CORPORATE_NAME_REQUIRED,
                    "법인명 입력 필요",
                    "corporateName"
            ));
        }
        if (!StringUtils.hasText(corporate.getBusinessRegistrationNo())) {
            missingItems.add(new KycMissingItemResponse(
                    BUSINESS_REGISTRATION_NO_REQUIRED,
                    "사업자등록번호 입력 필요",
                    "businessRegistrationNo"
            ));
        }
        if (!StringUtils.hasText(corporate.getRepresentativeName())) {
            missingItems.add(new KycMissingItemResponse(
                    REPRESENTATIVE_REQUIRED,
                    "대표자 정보 입력 필요",
                    "representativeName"
            ));
        }
        if (!StringUtils.hasText(kycApplication.getCorporateTypeCode())) {
            missingItems.add(new KycMissingItemResponse(
                    CORPORATE_TYPE_REQUIRED,
                    "법인 유형 선택 필요",
                    "corporateTypeCode"
            ));
        }
        if (kycApplication.getOriginalDocumentStoreOption() == null) {
            missingItems.add(new KycMissingItemResponse(
                    DOCUMENT_STORE_OPTION_REQUIRED,
                    "원본서류 저장 옵션 선택 필요",
                    "documentStoreOption"
            ));
        }

        Set<String> uploadedDocumentTypeCodes = getUploadedDocumentTypeCodes(documents); // 업로드 문서 유형 코드 목록
        for (RequiredDocumentPolicyProvider.RequiredDocumentPolicy policy
                : requiredDocumentPolicyProvider.getRequiredDocuments(kycApplication.getCorporateTypeCode())) {
            if (!uploadedDocumentTypeCodes.contains(policy.documentTypeCode())) {
                missingItems.add(new KycMissingItemResponse(
                        DOCUMENT_REQUIRED,
                        policy.documentTypeName() + " 업로드가 필요합니다.",
                        policy.documentTypeCode()
                ));
            }
        }

        return List.copyOf(missingItems);
    }

    // 제출 가능 여부 판단
    private boolean isSubmittable(
            List<KycMissingItemResponse> missingItems // 누락 항목 목록
    ) {
        return missingItems == null || missingItems.isEmpty();
    }

    // 제출 가능 여부 검증
    private void validateSubmittable(
            KycApplicationSummaryResponse summary // 제출 전 요약 정보
    ) {
        if (!summary.submittable()) {
            boolean documentMissing = summary.missingItems().stream()
                    .anyMatch(item -> DOCUMENT_REQUIRED.equals(item.code())); // 필수서류 누락 여부
            throw new ApiException(documentMissing ? ErrorCode.DOCUMENT_REQUIRED_MISSING : ErrorCode.INVALID_REQUEST);
        }
    }

    // 업로드 문서 유형 코드 목록 생성
    private Set<String> getUploadedDocumentTypeCodes(
            List<KycDocument> documents // 업로드 문서 목록
    ) {
        return documents == null ? Set.of() : documents.stream()
                .map(KycDocument::getDocumentTypeCode)
                .collect(Collectors.toSet());
    }

    // 사용자 소유 KYC 조회
    private KycApplication findOwnedKyc(
            Long userId, // 사용자 ID
            Long kycId // KYC 요청 ID
    ) {
        KycApplication kycApplication = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        if (!kycApplication.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.KYC_ACCESS_DENIED);
        }
        return kycApplication;
    }

    // 사용자 소유 법인 조회
    private Corporate findOwnedCorporate(
            Long userId, // 사용자 ID
            Long corporateId // 법인 ID
    ) {
        Corporate corporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        if (!corporate.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.CORPORATE_ACCESS_DENIED);
        }
        return corporate;
    }

    // KYC 문서 응답 변환
    private KycDocumentResponse toDocumentResponse(
            KycDocument kycDocument // KYC 문서
    ) {
        return new KycDocumentResponse(
                kycDocument.getDocumentId(),
                kycDocument.getKycId(),
                kycDocument.getDocumentTypeCode(),
                kycDocument.getFileName(),
                kycDocument.getMimeType(),
                kycDocument.getFileSize(),
                kycDocument.getDocumentHash(),
                kycDocument.getUploadStatus().name(),
                kycDocument.getUploadedAt()
        );
    }

    // Core AI 심사 요청 DTO 생성
    private CoreAiReviewRequest buildAiReviewRequest(
            String coreRequestId, // Core 요청 ID
            KycApplicationSummaryResponse summary, // KYC 제출 요약 정보
            LocalDateTime requestedAt, // 요청 시각
            String callbackUrl // Callback URL
    ) {
        return new CoreAiReviewRequest(
                coreRequestId,
                summary.kycId(),
                summary.corporateId(),
                summary.businessRegistrationNo(),
                summary.corporateName(),
                summary.representativeName(),
                buildAiReviewDocuments(summary.documents()),
                callbackUrl,
                requestedAt
        );
    }

    // Core AI 심사 문서 목록 생성
    private List<CoreAiReviewRequest.CoreAiReviewDocumentRequest> buildAiReviewDocuments(
            List<KycDocumentResponse> documents // 제출 문서 목록
    ) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
                .filter(document -> KyvcEnums.DocumentUploadStatus.UPLOADED.name().equals(document.uploadStatus()))
                .map(document -> new CoreAiReviewRequest.CoreAiReviewDocumentRequest(
                        document.documentId(),
                        document.documentTypeCode(),
                        document.documentHash(),
                        document.mimeType(),
                        document.fileSize()
                ))
                .toList();
    }

    // Core AI 심사 Callback URL 생성
    private String buildAiReviewCallbackUrl(
            String coreRequestId // Core 요청 ID
    ) {
        String callbackBaseUrl = coreInternalProperties.getCallbackBaseUrl(); // Callback 기준 URL
        if (!StringUtils.hasText(callbackBaseUrl) || !StringUtils.hasText(coreRequestId)) {
            return null;
        }
        String normalizedCallbackBaseUrl = callbackBaseUrl.endsWith("/")
                ? callbackBaseUrl.substring(0, callbackBaseUrl.length() - 1)
                : callbackBaseUrl;
        return normalizedCallbackBaseUrl + "/api/internal/core/ai-reviews/" + coreRequestId + "/callback";
    }

    // JSON 직렬화
    private String toJson(
            Object value // 직렬화 대상
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            logEventLogger.error("core.call.failed", "Core payload serialization failed", exception);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    // KYC 제출 로그 필드 생성
    private Map<String, Object> createSubmitLogFields(
            KycApplication kycApplication, // KYC 엔티티
            String coreRequestId // Core 요청 ID
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("kycId", kycApplication.getKycId());
        fields.put("corporateId", kycApplication.getCorporateId());
        fields.put("coreRequestId", coreRequestId);
        fields.put("kycStatus", enumName(kycApplication.getKycStatus()));
        fields.put("aiReviewStatus", enumName(kycApplication.getAiReviewStatus()));
        return fields;
    }

    // enum 이름 변환
    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // KYC 요청 ID 검증
    private void validateKycId(
            Long kycId // KYC 요청 ID
    ) {
        if (kycId == null || kycId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }
}
