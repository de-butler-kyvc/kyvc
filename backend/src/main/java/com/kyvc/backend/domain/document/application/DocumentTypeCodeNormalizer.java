package com.kyvc.backend.domain.document.application;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Locale;

// 문서 유형 코드 정규화 유틸
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentTypeCodeNormalizer {

    public static String normalize(
            String documentTypeCode // 원본 문서 유형 코드
    ) {
        if (!StringUtils.hasText(documentTypeCode)) {
            return null;
        }
        String normalized = documentTypeCode.trim().toUpperCase(Locale.ROOT); // 정규화 문서 유형 코드
        return switch (normalized) {
            case "CORPORATE_REGISTRATION" -> "CORPORATE_REGISTRY";
            case "SHAREHOLDER_LIST" -> "SHAREHOLDER_REGISTRY";
            case "ARTICLES_OF_INCORPORATION" -> "ARTICLES_OF_ASSOCIATION";
            case "REPRESENTATIVE_ID" -> "REPRESENTATIVE_PROOF_DOCUMENT";
            case "CORPORATE_SEAL_CERTIFICATE" -> "SEAL_CERTIFICATE";
            default -> normalized;
        };
    }
}
