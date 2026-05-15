package com.kyvc.backend.domain.core.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Core 요청 Payload 민감정보 마스킹
@Component
@RequiredArgsConstructor
public class CorePayloadSanitizer {

    private static final String MASKED_VALUE = "[MASKED]";
    private static final String UNPARSABLE_PAYLOAD = "[UNPARSABLE_PAYLOAD]";
    private static final String TRUNCATED_SUFFIX = "...[TRUNCATED]";
    private static final int MAX_TEXT_LENGTH = 2_000;
    private static final Set<String> EXACT_SENSITIVE_FIELDS = Set.of(
            "vc",
            "vp",
            "credential",
            "credentialpayloadjson",
            "vcpayloadjson",
            "presentation",
            "sdjwtkb",
            "verifiablecredential",
            "verifiablepresentation",
            "claims",
            "prompt",
            "businessnumber",
            "businessregistrationno",
            "businessregistrationnumber",
            "corporateregistrationnumber",
            "corporatename",
            "applicantname",
            "representativename",
            "representativephone",
            "representativeemail",
            "declaredrepresentative",
            "declaredbeneficialowners",
            "agentname",
            "agentphone",
            "agentemail",
            "agentauthorityscope",
            "contentbase64",
            "textcontent",
            "extracted",
            "holderdid",
            "holderaccount",
            "holderxrpladdress",
            "issueraccount",
            "issuerseed",
            "issuerverificationmethodid"
    );

    private final ObjectMapper objectMapper;

    public String sanitizePayload(
            String payloadJson // 저장 대상 Payload JSON
    ) {
        return sanitizePayload(payloadJson, true);
    }

    public String sanitizeAiReviewResponsePayload(
            String payloadJson // Core AI 심사 응답 JSON
    ) {
        return sanitizePayload(payloadJson, false);
    }

    private String sanitizePayload(
            String payloadJson, // 저장 대상 Payload JSON
            boolean maskClaims // claims 마스킹 여부
    ) {
        if (!StringUtils.hasText(payloadJson)) {
            return payloadJson;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(payloadJson);
            sanitizeNode(rootNode, maskClaims);
            return objectMapper.writeValueAsString(rootNode);
        } catch (JsonProcessingException exception) {
            return UNPARSABLE_PAYLOAD;
        }
    }

    public String sanitizeText(
            String value // 저장 대상 텍스트
    ) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (looksLikeJson(value)) {
            return sanitizePayload(value);
        }
        return truncateIfNeeded(value.trim());
    }

    private void sanitizeNode(
            JsonNode node, // 마스킹 대상 JSON 노드
            boolean maskClaims // claims 마스킹 여부
    ) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            sanitizeObjectNode((ObjectNode) node, maskClaims);
            return;
        }
        if (node.isArray()) {
            sanitizeArrayNode((ArrayNode) node, maskClaims);
        }
    }

    private void sanitizeObjectNode(
            ObjectNode objectNode, // 마스킹 대상 Object 노드
            boolean maskClaims // claims 마스킹 여부
    ) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();
            if (isSensitiveField(fieldName, maskClaims)) {
                objectNode.put(fieldName, MASKED_VALUE);
                continue;
            }
            if (fieldValue != null && (fieldValue.isObject() || fieldValue.isArray())) {
                sanitizeNode(fieldValue, maskClaims);
                continue;
            }
            if (fieldValue != null && fieldValue.isTextual()) {
                objectNode.put(fieldName, truncateIfNeeded(fieldValue.asText()));
            }
        }
    }

    private void sanitizeArrayNode(
            ArrayNode arrayNode, // 마스킹 대상 Array 노드
            boolean maskClaims // claims 마스킹 여부
    ) {
        for (int index = 0; index < arrayNode.size(); index++) {
            JsonNode itemNode = arrayNode.get(index);
            if (itemNode != null && itemNode.isTextual()) {
                arrayNode.set(index, objectMapper.getNodeFactory().textNode(truncateIfNeeded(itemNode.asText())));
                continue;
            }
            sanitizeNode(itemNode, maskClaims);
        }
    }

    private boolean isSensitiveField(
            String fieldName, // JSON 필드명
            boolean maskClaims // claims 마스킹 여부
    ) {
        String normalized = normalizeFieldName(fieldName);
        if (normalized == null) {
            return true;
        }
        return isExactSensitiveField(normalized, maskClaims)
                || normalized.contains("token")
                || normalized.contains("authorization")
                || normalized.contains("cookie")
                || normalized.contains("jwt")
                || normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("privatekey")
                || normalized.contains("signingkey")
                || normalized.contains("documentcontent")
                || normalized.contains("filecontent")
                || normalized.contains("rawpayload")
                || normalized.contains("rawdocument");
    }

    private boolean isExactSensitiveField(
            String normalizedFieldName, // 정규화 필드명
            boolean maskClaims // claims 마스킹 여부
    ) {
        if (!maskClaims && "claims".equals(normalizedFieldName)) {
            return false;
        }
        return EXACT_SENSITIVE_FIELDS.contains(normalizedFieldName);
    }

    private String normalizeFieldName(
            String fieldName // 원본 필드명
    ) {
        if (!StringUtils.hasText(fieldName)) {
            return null;
        }
        return fieldName.replace("_", "")
                .replace("-", "")
                .replace(".", "")
                .toLowerCase(Locale.ROOT);
    }

    private boolean looksLikeJson(
            String value // 원본 문자열
    ) {
        String trimmed = value.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private String truncateIfNeeded(
            String value // 원본 문자열
    ) {
        if (value == null || value.length() <= MAX_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TEXT_LENGTH) + TRUNCATED_SUFFIX;
    }
}
