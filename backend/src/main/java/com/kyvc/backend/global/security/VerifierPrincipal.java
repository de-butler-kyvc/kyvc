package com.kyvc.backend.global.security;

import java.time.LocalDateTime;

// Verifier API Key 인증 주체
public record VerifierPrincipal(
        Long verifierId, // Verifier ID
        Long apiKeyId, // API Key ID
        String verifierName, // Verifier 이름
        String verifierStatus, // Verifier 상태
        String apiKeyPrefix, // API Key 표시 prefix
        LocalDateTime apiKeyLastUsedAt // API Key 마지막 사용 일시
) {
}
