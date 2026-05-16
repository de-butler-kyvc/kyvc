package com.kyvc.backend.domain.core.application;

import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Core 문서 증빙 정책 유틸
public final class CoreDocumentEvidencePolicy {

    public static final String DOCUMENT_EVIDENCE_DISCLOSURE = "documentEvidence[]"; // 문서 증빙 disclosure path
    public static final String ORIGINAL_POLICY_REQUIRED = "REQUIRED"; // 원본 첨부 필수 정책
    public static final String SUBMISSION_MODE_ATTACHED_ORIGINAL = "ATTACHED_ORIGINAL"; // 원본 첨부 제출 모드

    private static final Map<String, String> CORE_DOCUMENT_TYPES = Map.ofEntries(
            Map.entry("BUSINESS_REGISTRATION", "KR_BUSINESS_REGISTRATION_CERTIFICATE"),
            Map.entry("CORPORATE_REGISTRATION", "KR_CORPORATE_REGISTER_FULL_CERTIFICATE"),
            Map.entry("SHAREHOLDER_LIST", "KR_SHAREHOLDER_REGISTER"),
            Map.entry("ARTICLES_OF_INCORPORATION", "KR_ARTICLES_OF_ASSOCIATION"),
            Map.entry("POWER_OF_ATTORNEY", "KR_POWER_OF_ATTORNEY"),
            Map.entry("CORPORATE_SEAL_CERTIFICATE", "KR_SEAL_CERTIFICATE"),
            Map.entry("REPRESENTATIVE_ID", "KR_ENTITY_REALNAME_CERTIFICATE"),
            Map.entry("AGENT_ID", "KR_ENTITY_REALNAME_CERTIFICATE")
    );

    private CoreDocumentEvidencePolicy() {
    }

    public static String toCoreDocumentType(
            String documentTypeCode // backend 문서 유형 코드
    ) {
        if (!StringUtils.hasText(documentTypeCode)) {
            return "UNKNOWN";
        }
        return CORE_DOCUMENT_TYPES.getOrDefault(documentTypeCode.trim().toUpperCase(java.util.Locale.ROOT), "UNKNOWN");
    }

    public static String requirementIdFor(
            String documentTypeCode // backend 문서 유형 코드
    ) {
        return switch (toCoreDocumentType(documentTypeCode)) {
            case "KR_BUSINESS_REGISTRATION_CERTIFICATE" -> "entity-realname-evidence";
            case "KR_CORPORATE_REGISTER_FULL_CERTIFICATE" -> "registry-evidence";
            case "KR_SHAREHOLDER_REGISTER" -> "ownership-evidence";
            default -> null;
        };
    }

    public static List<String> evidenceFor(
            String documentTypeCode // backend 문서 유형 코드
    ) {
        if (!StringUtils.hasText(documentTypeCode)) {
            return List.of();
        }
        return switch (documentTypeCode.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "BUSINESS_REGISTRATION" -> List.of("legalEntity.name", "legalEntity.registrationNumber");
            case "CORPORATE_REGISTRATION" -> List.of("legalEntity.registrationNumber", "representative.name");
            case "SHAREHOLDER_LIST" -> List.of("beneficialOwners[]");
            case "ARTICLES_OF_INCORPORATION" -> List.of("legalEntity.type", "establishmentPurpose");
            case "POWER_OF_ATTORNEY", "CORPORATE_SEAL_CERTIFICATE" -> List.of("delegate", "delegation");
            case "REPRESENTATIVE_ID" -> List.of("representative");
            case "AGENT_ID" -> List.of("delegate");
            default -> List.of();
        };
    }

    public static String digestSRI(
            byte[] content // 원본 파일 bytes
    ) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-384").digest(content);
            return "sha384-" + Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-384 digest algorithm unavailable", exception);
        }
    }

    public static boolean requiresDocumentEvidence(
            List<String> requestedClaims // 요청 claim 목록
    ) {
        return requestedClaims != null
                && requestedClaims.stream()
                .anyMatch(claim -> DOCUMENT_EVIDENCE_DISCLOSURE.equals(normalizeClaim(claim))
                        || "documentEvidence".equals(normalizeClaim(claim)));
    }

    public static List<String> requiredDisclosures(
            List<String> requestedClaims // 요청 claim 목록
    ) {
        if (requestedClaims == null) {
            return List.of();
        }
        return requestedClaims.stream()
                .map(CoreDocumentEvidencePolicy::normalizeClaim)
                .filter(StringUtils::hasText)
                .map(claim -> "documentEvidence".equals(claim) ? DOCUMENT_EVIDENCE_DISCLOSURE : claim)
                .distinct()
                .toList();
    }

    public static List<Map<String, Object>> attachedOriginalDocumentRules(
            List<String> requestedClaims // 요청 claim 목록
    ) {
        if (!requiresDocumentEvidence(requestedClaims)) {
            return List.of();
        }
        return List.of(
                documentRule("entity-realname-evidence", List.of("KR_BUSINESS_REGISTRATION_CERTIFICATE")),
                documentRule("registry-evidence", List.of("KR_CORPORATE_REGISTER_FULL_CERTIFICATE")),
                documentRule("ownership-evidence", List.of("KR_SHAREHOLDER_REGISTER"))
        );
    }

    public static List<String> financeKycRequiredDisclosures(
            List<String> requestedClaims // 금융사 요청 Claim 목록
    ) {
        if (requestedClaims == null) {
            return List.of();
        }
        return requestedClaims.stream()
                .map(CoreDocumentEvidencePolicy::toFinanceKycDisclosurePath)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private static Map<String, Object> documentRule(
            String id, // Core document rule ID
            List<String> oneOf // 허용 Core 문서 유형 목록
    ) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", id);
        rule.put("required", true);
        rule.put("oneOf", oneOf);
        rule.put("originalPolicy", ORIGINAL_POLICY_REQUIRED);
        return rule;
    }

    private static String normalizeClaim(
            String claim // 요청 claim path
    ) {
        return StringUtils.hasText(claim) ? claim.trim() : null;
    }
    private static String toFinanceKycDisclosurePath(
            String claim // 금융사 요청 Claim
    ) {
        String normalized = normalizeClaim(claim);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return switch (normalized.toLowerCase(java.util.Locale.ROOT)) {
            case "corporatename", "legalentity.name" -> "legalEntity.name";
            case "businessregistrationno", "businessregistrationnumber", "legalentity.registrationnumber" ->
                    "legalEntity.registrationNumber";
            case "representativename", "representative.name" -> "representative.name";
            default -> null;
        };
    }
}
