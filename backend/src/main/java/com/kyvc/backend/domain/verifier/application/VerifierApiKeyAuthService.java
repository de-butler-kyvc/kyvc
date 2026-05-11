package com.kyvc.backend.domain.verifier.application;

import com.kyvc.backend.domain.verifier.domain.Verifier;
import com.kyvc.backend.domain.verifier.domain.VerifierApiKey;
import com.kyvc.backend.domain.verifier.repository.VerifierApiKeyRepository;
import com.kyvc.backend.domain.verifier.repository.VerifierRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.security.VerifierPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

// Verifier API Key 인증 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class VerifierApiKeyAuthService {

    private final VerifierApiKeyRepository verifierApiKeyRepository;
    private final VerifierRepository verifierRepository;

    // API Key 인증
    public VerifierPrincipal authenticate(
            String apiKey // API Key 원문
    ) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ApiException(ErrorCode.VERIFIER_API_KEY_REQUIRED);
        }

        String apiKeyHash = TokenHashUtil.sha256(apiKey.trim()); // API Key hash
        VerifierApiKey verifierApiKey = verifierApiKeyRepository.findByApiKeyHash(apiKeyHash)
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFIER_API_KEY_INVALID));

        LocalDateTime now = LocalDateTime.now(); // 검증 기준 일시
        if (verifierApiKey.isExpired(now)) {
            throw new ApiException(ErrorCode.VERIFIER_API_KEY_EXPIRED);
        }
        if (!verifierApiKey.isActive()) {
            throw new ApiException(ErrorCode.VERIFIER_API_KEY_INACTIVE);
        }

        Verifier verifier = verifierRepository.findById(verifierApiKey.getVerifierId())
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFIER_NOT_FOUND));
        if (!verifier.isActive()) {
            throw new ApiException(ErrorCode.VERIFIER_INACTIVE);
        }

        LocalDateTime previousLastUsedAt = verifierApiKey.getLastUsedAt(); // 응답 표시용 마지막 사용 일시
        verifierApiKey.markUsed(now);
        verifierApiKeyRepository.save(verifierApiKey);

        return new VerifierPrincipal(
                verifier.getVerifierId(),
                verifierApiKey.getApiKeyId(),
                verifier.getVerifierName(),
                verifier.getVerifierStatus() == null ? null : verifier.getVerifierStatus().name(),
                verifierApiKey.getApiKeyPrefix(),
                previousLastUsedAt
        );
    }
}
