package com.kyvc.backend.domain.kyc.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.config.CoreProperties;
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

// KYC ??뽱뀱 ??뺥돩??
@Service
@Transactional
@RequiredArgsConstructor
public class KycSubmissionService {

    private static final String CORPORATE_NAME_REQUIRED = "CORPORATE_NAME_REQUIRED"; // 甕곕벡?ㅿ쭗??袁⑥뵭 ?꾨뗀諭?
    private static final String BUSINESS_REGISTRATION_NO_REQUIRED = "BUSINESS_REGISTRATION_NO_REQUIRED"; // ??毓?癒?쾻嚥≪빖苡???袁⑥뵭 ?꾨뗀諭?
    private static final String REPRESENTATIVE_REQUIRED = "REPRESENTATIVE_REQUIRED"; // ????뽰쁽 ?類ｋ궖 ?袁⑥뵭 ?꾨뗀諭?
    private static final String CORPORATE_TYPE_REQUIRED = "CORPORATE_TYPE_REQUIRED"; // 甕곕벡???醫륁굨 ?袁⑥뵭 ?꾨뗀諭?
    private static final String DOCUMENT_STORE_OPTION_REQUIRED = "DOCUMENT_STORE_OPTION_REQUIRED"; // ?癒?궚??뺤첒 ?????????袁⑥뵭 ?꾨뗀諭?
    private static final String DOCUMENT_REQUIRED = "DOCUMENT_REQUIRED"; // ?袁⑸땾??뺤첒 ?袁⑥뵭 ?꾨뗀諭?
    private static final String AI_REVIEW_REQUESTED_MESSAGE = "KYC 신청이 제출되었고 AI 심사가 요청되었습니다.";

    private final KycApplicationRepository kycApplicationRepository;
    private final CorporateRepository corporateRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final RequiredDocumentPolicyProvider requiredDocumentPolicyProvider;
    private final CoreRequestService coreRequestService;
    private final CoreAdapter coreAdapter;
    private final CoreProperties coreProperties;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    // KYC ??뽱뀱 ?遺용튋 鈺곌퀬??
    @Transactional(readOnly = true)
    public KycApplicationSummaryResponse getSummary(
            Long userId, // ?????ID
            Long kycId // KYC ?遺욧퍕 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        return buildSummary(userId, findOwnedKyc(userId, kycId));
    }

    // KYC ??뽱뀱
    public KycSubmitResponse submit(
            Long userId, // ?????ID
            Long kycId // KYC ?遺욧퍕 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);

        KycApplication kycApplication = findOwnedKyc(userId, kycId); // ????????? KYC
        if (!kycApplication.isDraft()) {
            throw new ApiException(ErrorCode.KYC_INVALID_STATUS);
        }

        KycApplicationSummaryResponse summary = buildSummary(userId, kycApplication); // ??뽱뀱 ???遺용튋 ?類ｋ궖
        validateSubmittable(summary);

        LocalDateTime submittedAt = LocalDateTime.now(); // ??뽱뀱 ??깅뻻
        String coreRequestId = null; // Core ?遺욧퍕 ID

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
            ); // Core AI ??沅?API 沃섎챷肉???怨밴묶??Stub ?遺욧퍕

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
            KycApplication savedApplication = kycApplicationRepository.save(kycApplication); // ?????袁⑥┷ KYC

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

    // ??뽱뀱 ???遺용튋 ??밴쉐
    private KycApplicationSummaryResponse buildSummary(
            Long userId, // ?????ID
            KycApplication kycApplication // KYC ?遺욧퍕 ?類ｋ궖
    ) {
        Corporate corporate = findOwnedCorporate(userId, kycApplication.getCorporateId()); // ???? 甕곕벡???類ｋ궖
        List<KycDocument> documents = kycDocumentRepository.findByKycId(kycApplication.getKycId()); // ??낆쨮???얜챷苑?筌뤴뫖以?
        List<KycDocumentResponse> documentResponses = documents.stream()
                .map(this::toDocumentResponse)
                .toList();
        List<RequiredDocumentResponse> requiredDocuments = buildRequiredDocuments(kycApplication, documents); // ?袁⑸땾??뺤첒 ?겸뫗????? 筌뤴뫖以?
        List<KycMissingItemResponse> missingItems = buildMissingItems(corporate, kycApplication, documents); // ?袁⑥뵭 ????筌뤴뫖以?
        boolean submittable = kycApplication.isDraft() && isSubmittable(missingItems); // ??뽱뀱 揶쎛?????

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

    // ?袁⑸땾??뺤첒 ?겸뫗????? 筌뤴뫖以???밴쉐
    private List<RequiredDocumentResponse> buildRequiredDocuments(
            KycApplication kycApplication, // KYC ?遺욧퍕 ?類ｋ궖
            List<KycDocument> documents // ??낆쨮???얜챷苑?筌뤴뫖以?
    ) {
        Set<String> uploadedDocumentTypeCodes = getUploadedDocumentTypeCodes(documents); // ??낆쨮???얜챷苑??醫륁굨 ?꾨뗀諭?筌뤴뫖以?
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

    // ?袁⑥뵭 ????筌뤴뫖以???밴쉐
    private List<KycMissingItemResponse> buildMissingItems(
            Corporate corporate, // 甕곕벡???類ｋ궖
            KycApplication kycApplication, // KYC ?遺욧퍕 ?類ｋ궖
            List<KycDocument> documents // ??낆쨮???얜챷苑?筌뤴뫖以?
    ) {
        Set<KycMissingItemResponse> missingItems = new LinkedHashSet<>(); // ?袁⑥뵭 ????筌뤴뫖以?

        if (!StringUtils.hasText(corporate.getCorporateName())) {
            missingItems.add(new KycMissingItemResponse(
                    CORPORATE_NAME_REQUIRED,
                    "甕곕벡?ㅿ쭗???낆젾 ?袁⑹뒄",
                    "corporateName"
            ));
        }
        if (!StringUtils.hasText(corporate.getBusinessRegistrationNo())) {
            missingItems.add(new KycMissingItemResponse(
                    BUSINESS_REGISTRATION_NO_REQUIRED,
                    "??毓?癒?쾻嚥≪빖苡????낆젾 ?袁⑹뒄",
                    "businessRegistrationNo"
            ));
        }
        if (!StringUtils.hasText(corporate.getRepresentativeName())) {
            missingItems.add(new KycMissingItemResponse(
                    REPRESENTATIVE_REQUIRED,
                    "????뽰쁽 ?類ｋ궖 ??낆젾 ?袁⑹뒄",
                    "representativeName"
            ));
        }
        if (!StringUtils.hasText(kycApplication.getCorporateTypeCode())) {
            missingItems.add(new KycMissingItemResponse(
                    CORPORATE_TYPE_REQUIRED,
                    "甕곕벡???醫륁굨 ?醫뤾문 ?袁⑹뒄",
                    "corporateTypeCode"
            ));
        }
        if (kycApplication.getOriginalDocumentStoreOption() == null) {
            missingItems.add(new KycMissingItemResponse(
                    DOCUMENT_STORE_OPTION_REQUIRED,
                    "?癒?궚??뺤첒 ?????????醫뤾문 ?袁⑹뒄",
                    "documentStoreOption"
            ));
        }

        Set<String> uploadedDocumentTypeCodes = getUploadedDocumentTypeCodes(documents); // ??낆쨮???얜챷苑??醫륁굨 ?꾨뗀諭?筌뤴뫖以?
        for (RequiredDocumentPolicyProvider.RequiredDocumentPolicy policy
                : requiredDocumentPolicyProvider.getRequiredDocuments(kycApplication.getCorporateTypeCode())) {
            if (!uploadedDocumentTypeCodes.contains(policy.documentTypeCode())) {
                missingItems.add(new KycMissingItemResponse(
                        DOCUMENT_REQUIRED,
                        policy.documentTypeName() + " ??낆쨮??? ?袁⑹뒄??몃빍??",
                        policy.documentTypeCode()
                ));
            }
        }

        return List.copyOf(missingItems);
    }

    // ??뽱뀱 揶쎛????? ?癒?뼊
    private boolean isSubmittable(
            List<KycMissingItemResponse> missingItems // ?袁⑥뵭 ????筌뤴뫖以?
    ) {
        return missingItems == null || missingItems.isEmpty();
    }

    // ??뽱뀱 揶쎛????? 野꺜筌?
    private void validateSubmittable(
            KycApplicationSummaryResponse summary // ??뽱뀱 ???遺용튋 ?類ｋ궖
    ) {
        if (!summary.submittable()) {
            boolean documentMissing = summary.missingItems().stream()
                    .anyMatch(item -> DOCUMENT_REQUIRED.equals(item.code())); // ?袁⑸땾??뺤첒 ?袁⑥뵭 ???
            throw new ApiException(documentMissing ? ErrorCode.DOCUMENT_REQUIRED_MISSING : ErrorCode.INVALID_REQUEST);
        }
    }

    // ??낆쨮???얜챷苑??醫륁굨 ?꾨뗀諭?筌뤴뫖以???밴쉐
    private Set<String> getUploadedDocumentTypeCodes(
            List<KycDocument> documents // ??낆쨮???얜챷苑?筌뤴뫖以?
    ) {
        return documents == null ? Set.of() : documents.stream()
                .map(KycDocument::getDocumentTypeCode)
                .collect(Collectors.toSet());
    }

    // ????????? KYC 鈺곌퀬??
    private KycApplication findOwnedKyc(
            Long userId, // ?????ID
            Long kycId // KYC ?遺욧퍕 ID
    ) {
        KycApplication kycApplication = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        if (!kycApplication.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.KYC_ACCESS_DENIED);
        }
        return kycApplication;
    }

    // ????????? 甕곕벡??鈺곌퀬??
    private Corporate findOwnedCorporate(
            Long userId, // ?????ID
            Long corporateId // 甕곕벡??ID
    ) {
        Corporate corporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        if (!corporate.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.CORPORATE_ACCESS_DENIED);
        }
        return corporate;
    }

    // KYC ?얜챷苑??臾먮뼗 癰궰??
    private KycDocumentResponse toDocumentResponse(
            KycDocument kycDocument // KYC ?얜챷苑?
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

    // Core AI ??沅??遺욧퍕 DTO ??밴쉐
    private CoreAiReviewRequest buildAiReviewRequest(
            String coreRequestId, // Core ?遺욧퍕 ID
            KycApplicationSummaryResponse summary, // KYC ??뽱뀱 ?遺용튋 ?類ｋ궖
            LocalDateTime requestedAt, // ?遺욧퍕 ??볦퍟
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

    // Core AI ??沅??얜챷苑?筌뤴뫖以???밴쉐
    private List<CoreAiReviewRequest.CoreAiReviewDocumentRequest> buildAiReviewDocuments(
            List<KycDocumentResponse> documents // ??뽱뀱 ?얜챷苑?筌뤴뫖以?
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

    // Core AI ??沅?Callback URL ??밴쉐
    private String buildAiReviewCallbackUrl(
            String coreRequestId // Core ?遺욧퍕 ID
    ) {
        String callbackBaseUrl = coreProperties.getCallbackBaseUrl(); // Callback 疫꿸퀣? URL
        if (!StringUtils.hasText(callbackBaseUrl) || !StringUtils.hasText(coreRequestId)) {
            return null;
        }
        String normalizedCallbackBaseUrl = callbackBaseUrl.endsWith("/")
                ? callbackBaseUrl.substring(0, callbackBaseUrl.length() - 1)
                : callbackBaseUrl;
        return normalizedCallbackBaseUrl + "/api/internal/core/ai-reviews/" + coreRequestId + "/callback";
    }

    // JSON 筌욊낮???
    private String toJson(
            Object value // 筌욊낮???????
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            logEventLogger.error("core.call.failed", "Core payload serialization failed", exception);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    // KYC ??뽱뀱 嚥≪뮄???袁⑤굡 ??밴쉐
    private Map<String, Object> createSubmitLogFields(
            KycApplication kycApplication, // KYC ?酉???
            String coreRequestId // Core ?遺욧퍕 ID
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("kycId", kycApplication.getKycId());
        fields.put("corporateId", kycApplication.getCorporateId());
        fields.put("coreRequestId", coreRequestId);
        fields.put("kycStatus", enumName(kycApplication.getKycStatus()));
        fields.put("aiReviewStatus", enumName(kycApplication.getAiReviewStatus()));
        return fields;
    }

    // enum ??已?癰궰??
    private String enumName(
            Enum<?> value // enum 揶?
    ) {
        return value == null ? null : value.name();
    }

    // ?????ID 野꺜筌?
    private void validateUserId(
            Long userId // ?????ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // KYC ?遺욧퍕 ID 野꺜筌?
    private void validateKycId(
            Long kycId // KYC ?遺욧퍕 ID
    ) {
        if (kycId == null || kycId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }
}


