package com.kyvc.backend.domain.verifier.application;

import com.kyvc.backend.domain.verifier.domain.Verifier;
import com.kyvc.backend.domain.verifier.dto.VerifierAppMeResponse;
import com.kyvc.backend.domain.verifier.repository.VerifierCallbackRepository;
import com.kyvc.backend.domain.verifier.repository.VerifierRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.security.VerifierPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Verifier 앱 정보 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VerifierAppService {

    private final VerifierRepository verifierRepository;
    private final VerifierCallbackRepository verifierCallbackRepository;

    // Verifier 본인 앱 정보 조회
    public VerifierAppMeResponse getMe(
            VerifierPrincipal principal // 인증된 Verifier 주체
    ) {
        Verifier verifier = verifierRepository.findById(principal.verifierId())
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFIER_NOT_FOUND));
        if (!verifier.isActive()) {
            throw new ApiException(ErrorCode.VERIFIER_INACTIVE);
        }
        String callbackUrl = verifierCallbackRepository.findActiveByVerifierId(verifier.getVerifierId())
                .map(callback -> callback.getCallbackUrl())
                .orElse(null);
        return new VerifierAppMeResponse(
                verifier.getVerifierId(),
                verifier.getVerifierName(),
                verifier.getVerifierStatus() == null ? null : verifier.getVerifierStatus().name(),
                principal.apiKeyPrefix(),
                callbackUrl,
                verifier.getCreatedAt(),
                principal.apiKeyLastUsedAt()
        );
    }
}
