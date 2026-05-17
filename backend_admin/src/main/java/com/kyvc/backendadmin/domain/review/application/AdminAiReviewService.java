package com.kyvc.backendadmin.domain.review.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kyvc.backendadmin.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backendadmin.domain.review.dto.AdminAiReviewDetailResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewAgentAuthorityResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewBeneficialOwnerResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewMismatchResponse;
import com.kyvc.backendadmin.domain.review.dto.KycReviewHistoryResponse;
import com.kyvc.backendadmin.domain.review.repository.AiReviewQueryRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Backend Admin AI 심사 결과 조회 유스케이스를 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class AdminAiReviewService {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "password_hash", "token", "token_hash", "api_key", "api_key_hash",
            "credential_salt", "credential_salt_hash", "vp_jwt", "vp_jwt_hash", "private_key",
            "secret", "verification_code", "verification_code_hash"
    );

    private final KycApplicationRepository kycApplicationRepository;
    private final AiReviewQueryRepository aiReviewQueryRepository;
    private final ObjectMapper objectMapper;

    /**
     * KYC 신청 ID를 기준으로 AI 심사 결과 상세 정보를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 심사 결과 상세 정보
     */
    @Transactional(readOnly = true)
    public AdminAiReviewDetailResponse getAiReviewDetail(Long kycId) {
        validateKycExists(kycId);
        AdminAiReviewDetailResponse response = aiReviewQueryRepository.findAiReviewDetail(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        return new AdminAiReviewDetailResponse(
                response.kycId(),
                response.aiReviewStatusCode(),
                response.aiReviewResultCode(),
                response.aiConfidenceScore(),
                response.aiReviewSummary(),
                maskDetailJson(response.aiReviewDetailJson()),
                maskDetailJson(response.coreAiAssessmentJson()),
                response.manualReviewReason(),
                response.aiReviewReasonCode(),
                response.updatedAt()
        );
    }

    /**
     * KYC 신청 ID를 기준으로 AI 문서 불일치 결과를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 문서 불일치 결과
     */
    @Transactional(readOnly = true)
    public AiReviewMismatchResponse getMismatches(Long kycId) {
        JsonNode detail = loadDetailJson(kycId);
        List<AiReviewMismatchResponse.Mismatch> mismatches = new ArrayList<>();
        for (JsonNode node : findArray(detail, "mismatches", "documentMismatches", "document_mismatches")) {
            mismatches.add(new AiReviewMismatchResponse.Mismatch(
                    text(node, "mismatchTypeCode", "typeCode", "fieldName"),
                    text(node, "mismatchTypeName", "typeName", "fieldName"),
                    text(node, "sourceDocumentTypeCode", "sourceA", "sourceDocument"),
                    text(node, "targetDocumentTypeCode", "sourceB", "targetDocument"),
                    text(node, "sourceValue", "valueA"),
                    text(node, "targetValue", "valueB"),
                    yn(node, "matchedYn", "matched"),
                    decimal(node, "confidenceScore", "confidence"),
                    text(node, "reason", "judgementReason", "basis")
            ));
        }
        return new AiReviewMismatchResponse(kycId, mismatches);
    }

    /**
     * KYC 신청 ID를 기준으로 AI 실제소유자 판단 결과를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 실제소유자 판단 결과
     */
    @Transactional(readOnly = true)
    public AiReviewBeneficialOwnerResponse getBeneficialOwners(Long kycId) {
        JsonNode detail = loadDetailJson(kycId);
        List<AiReviewBeneficialOwnerResponse.BeneficialOwner> owners = new ArrayList<>();
        for (JsonNode node : findArray(detail, "beneficialOwners", "beneficial_owners", "owners")) {
            owners.add(new AiReviewBeneficialOwnerResponse.BeneficialOwner(
                    text(node, "ownerName", "name"),
                    decimal(node, "ownershipRatio", "shareRatio", "ratio"),
                    text(node, "controlTypeCode", "controlType"),
                    yn(node, "beneficialOwnerYn", "isBeneficialOwner", "beneficialOwner"),
                    decimal(node, "confidenceScore", "confidence"),
                    text(node, "judgementReason", "basis", "reason")
            ));
        }
        return new AiReviewBeneficialOwnerResponse(kycId, owners);
    }

    /**
     * KYC 신청 ID를 기준으로 AI 대리인 권한 판단 결과를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 대리인 권한 판단 결과
     */
    @Transactional(readOnly = true)
    public AiReviewAgentAuthorityResponse getAgentAuthority(Long kycId) {
        validateKycExists(kycId);
        AiReviewAgentAuthorityResponse.AgentAuthority base = aiReviewQueryRepository
                .findAgentAuthorityBase(kycId)
                .orElse(new AiReviewAgentAuthorityResponse.AgentAuthority(null, null, null, null, null, null, null));

        JsonNode detail = readDetailJson(findRawDetail(kycId));
        JsonNode node = firstObject(detail, "agentAuthority", "agent_authority", "agentAuthorities");
        if (node == null || node.isMissingNode() || node.isNull()) {
            return new AiReviewAgentAuthorityResponse(kycId, base);
        }
        return new AiReviewAgentAuthorityResponse(kycId, new AiReviewAgentAuthorityResponse.AgentAuthority(
                firstText(base.agentName(), text(node, "agentName", "name")),
                firstText(base.authorityScope(), text(node, "authorityScope", "scope")),
                yn(node, "signatureVerifiedYn", "signatureVerified", "hasSignature"),
                yn(node, "sealVerifiedYn", "sealVerified", "hasSeal"),
                yn(node, "authorityValidYn", "authorityValid", "validAuthority"),
                decimal(node, "confidenceScore", "confidence"),
                text(node, "judgementReason", "reason", "basis")
        ));
    }

    /**
     * KYC 신청 ID를 기준으로 심사 이력을 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return KYC 심사 이력 목록
     */
    @Transactional(readOnly = true)
    public List<KycReviewHistoryResponse> getReviewHistories(Long kycId) {
        validateKycExists(kycId);
        return aiReviewQueryRepository.findReviewHistories(kycId);
    }

    private JsonNode loadDetailJson(Long kycId) {
        validateKycExists(kycId);
        return readDetailJson(findRawDetail(kycId));
    }

    private String findRawDetail(Long kycId) {
        return aiReviewQueryRepository.findAiReviewDetail(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND))
                .aiReviewDetailJson();
    }

    private void validateKycExists(Long kycId) {
        kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
    }

    private String maskDetailJson(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return rawJson;
        }
        JsonNode node = readDetailJson(rawJson);
        maskSensitiveValues(node);
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.AI_REVIEW_DETAIL_JSON_INVALID, exception);
        }
    }

    private JsonNode readDetailJson(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.AI_REVIEW_DETAIL_JSON_INVALID, exception);
        }
    }

    private void maskSensitiveValues(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<String> fieldNames = objectNode.fieldNames();
            List<String> names = new ArrayList<>();
            fieldNames.forEachRemaining(names::add);
            for (String name : names) {
                if (isSensitiveKey(name)) {
                    objectNode.put(name, "***");
                } else {
                    maskSensitiveValues(objectNode.get(name));
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::maskSensitiveValues);
        }
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.stream().anyMatch(normalized::contains);
    }

    private List<JsonNode> findArray(JsonNode root, String... keys) {
        JsonNode node = firstNode(root, keys);
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<JsonNode> values = new ArrayList<>();
        node.forEach(values::add);
        return values;
    }

    private JsonNode firstObject(JsonNode root, String... keys) {
        JsonNode node = firstNode(root, keys);
        if (node == null) {
            return null;
        }
        if (node.isArray() && !node.isEmpty()) {
            return node.get(0);
        }
        return node;
    }

    private JsonNode firstNode(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.path(key);
            if (!node.isMissingNode() && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private String text(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    private BigDecimal decimal(JsonNode node, String... keys) {
        String value = text(node, keys);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new ApiException(ErrorCode.AI_REVIEW_DETAIL_JSON_INVALID, exception);
        }
    }

    private String yn(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (value.isMissingNode() || value.isNull()) {
                continue;
            }
            if (value.isBoolean()) {
                return value.asBoolean() ? "Y" : "N";
            }
            String text = value.asText();
            if ("true".equalsIgnoreCase(text)) {
                return "Y";
            }
            if ("false".equalsIgnoreCase(text)) {
                return "N";
            }
            return text;
        }
        return null;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
