package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreDocumentEvidencePolicy;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorage;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Credential claims 조립기
@Component
@RequiredArgsConstructor
public class CredentialClaimsAssembler {

    private final CoreProperties coreProperties;
    private final LogEventLogger logEventLogger;
    private final ObjectMapper objectMapper;
    private final KycDocumentRepository kycDocumentRepository;
    private final DocumentStorage documentStorage;

    // KYC 신청 기준 claims 조립
    public Map<String, Object> assemble(
            KycApplication kycApplication // KYC 신청
    ) {
        if (kycApplication == null || kycApplication.getKycId() == null) {
            throw new ApiException(ErrorCode.KYC_NOT_FOUND);
        }

        Map<String, Object> detail = parseAiReviewDetailJson(kycApplication.getAiReviewDetailJson());
        Map<String, Object> claims = asObjectMap(detail.get("claims"));
        if (claims.isEmpty()) {
            return fallbackClaimsIfEnabled(kycApplication.getKycId());
        }
        applyDocumentEvidence(claims, kycApplication.getKycId());
        return claims;
    }

    private void applyDocumentEvidence(
            Map<String, Object> claims, // Credential claims
            Long kycId // KYC 신청 ID
    ) {
        List<Map<String, Object>> documentEvidence = kycDocumentRepository.findByKycId(kycId)
                .stream()
                .filter(KycDocument::isPreviewAvailable)
                .map(this::toDocumentEvidence)
                .toList();
        if (!documentEvidence.isEmpty()) {
            claims.put("documentEvidence", documentEvidence);
        }
    }

    private Map<String, Object> toDocumentEvidence(
            KycDocument document // KYC 제출 문서
    ) {
        byte[] content = loadDocumentBytes(document);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("documentId", "urn:kyvc:doc:" + document.getDocumentId());
        evidence.put("documentType", CoreDocumentEvidencePolicy.toCoreDocumentType(document.getDocumentTypeCode()));
        evidence.put("documentClass", document.getDocumentTypeCode());
        evidence.put("digestSRI", digestSRI(content));
        evidence.put("mediaType", document.getMimeType());
        evidence.put("byteSize", document.getFileSize());
        evidence.put("hashInput", "original-file-bytes");
        evidence.put("fileName", document.getFileName());
        evidence.put("evidenceFor", CoreDocumentEvidencePolicy.evidenceFor(document.getDocumentTypeCode()));
        return evidence;
    }

    private byte[] loadDocumentBytes(
            KycDocument document // KYC 제출 문서
    ) {
        try (InputStream inputStream = documentStorage.load(document.getFilePath()).resource().getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_FILE_NOT_FOUND, exception);
        }
    }

    private String digestSRI(
            byte[] content // 원본 파일 bytes
    ) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-384").digest(content);
            return "sha384-" + Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private Map<String, Object> parseAiReviewDetailJson(
            String detailJson // AI 심사 상세 JSON
    ) {
        if (!StringUtils.hasText(detailJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(detailJson, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.CREDENTIAL_CLAIMS_REQUIRED_DATA_MISSING, exception);
        }
    }

    private Map<String, Object> asObjectMap(
            Object value // Map 변환 대상
    ) {
        if (!(value instanceof Map<?, ?> mapValue)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        mapValue.forEach((key, mapEntryValue) -> {
            if (key != null) {
                result.put(String.valueOf(key), mapEntryValue);
            }
        });
        return result;
    }

    private Map<String, Object> fallbackClaimsIfEnabled(
            Long kycId // KYC 신청 ID
    ) {
        if (coreProperties.isDevSeedEnabled()) {
            logEventLogger.warn(
                    "credential.claims.ai_review_missing",
                    "Credential claims AI 심사 결과 누락",
                    Map.of("kycId", kycId)
            );
        }
        throw new ApiException(ErrorCode.CREDENTIAL_CLAIMS_REQUIRED_DATA_MISSING);
    }
}
