package com.kyvc.backendadmin.domain.verifier.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.verifier.dto.AdminVerifierDtos;
import com.kyvc.backendadmin.domain.verifier.repository.VerifierQueryRepository;
import com.kyvc.backendadmin.domain.verifier.repository.VerifierRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.jwt.TokenHashUtil;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Verifier API Key 발급, 회전, 폐기 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminVerifierApiKeyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final VerifierRepository verifierRepository;
    private final VerifierQueryRepository verifierQueryRepository;
    private final AdminVerifierQueryService queryService;
    private final AuditLogWriter auditLogWriter;

    /**
     * Verifier API Key 목록을 조회합니다.
     *
     * @param verifierId Verifier ID
     * @return API Key 목록
     */
    @Transactional(readOnly = true)
    public List<AdminVerifierDtos.ApiKeyResponse> getKeys(Long verifierId) {
        queryService.findVerifier(verifierId);
        return verifierQueryRepository.findApiKeys(verifierId);
    }

    /**
     * API Key를 발급하고 원문 secret을 최초 1회 반환합니다.
     *
     * @param verifierId Verifier ID
     * @param request 발급 요청
     * @return API Key 발급 응답
     */
    @Transactional
    public AdminVerifierDtos.ApiKeySecretResponse create(Long verifierId, AdminVerifierDtos.ApiKeyCreateRequest request) {
        queryService.findVerifier(verifierId);
        Secret secret = generateSecret();
        Long keyId = verifierRepository.createApiKey(verifierId, request.name(), secret.prefix(), TokenHashUtil.sha256(secret.raw()), request.expiresAt());
        audit("VERIFIER_API_KEY_CREATED", verifierId, "API Key 발급. keyId=%d".formatted(keyId));
        return new AdminVerifierDtos.ApiKeySecretResponse(keyId, secret.prefix(), secret.raw(), request.expiresAt());
    }

    /**
     * 기존 API Key를 ROTATED 처리하고 신규 Key를 발급합니다.
     *
     * @param verifierId Verifier ID
     * @param keyId 기존 API Key ID
     * @return 신규 API Key 발급 응답
     */
    @Transactional
    public AdminVerifierDtos.ApiKeySecretResponse rotate(Long verifierId, Long keyId) {
        VerifierRepository.ApiKeyRow oldKey = verifierRepository.findApiKey(verifierId, keyId)
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFIER_API_KEY_NOT_FOUND));
        verifierRepository.updateApiKeyStatus(keyId, "ROTATED", LocalDateTime.now());
        Secret secret = generateSecret();
        Long newKeyId = verifierRepository.createApiKey(verifierId, oldKey.keyName(), secret.prefix(), TokenHashUtil.sha256(secret.raw()), oldKey.expiresAt());
        audit("VERIFIER_API_KEY_ROTATED", verifierId, "API Key 회전. oldKeyId=%d, newKeyId=%d".formatted(keyId, newKeyId));
        return new AdminVerifierDtos.ApiKeySecretResponse(newKeyId, secret.prefix(), secret.raw(), oldKey.expiresAt());
    }

    /**
     * API Key를 폐기합니다.
     *
     * @param verifierId Verifier ID
     * @param keyId API Key ID
     * @param request 폐기 요청
     * @return 폐기된 API Key 목록용 응답
     */
    @Transactional
    public AdminVerifierDtos.ApiKeyResponse revoke(Long verifierId, Long keyId, AdminVerifierDtos.ApiKeyRevokeRequest request) {
        verifierRepository.findApiKey(verifierId, keyId)
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFIER_API_KEY_NOT_FOUND));
        verifierRepository.updateApiKeyStatus(keyId, "REVOKED", LocalDateTime.now());
        audit("VERIFIER_API_KEY_REVOKED", verifierId, "API Key 폐기. keyId=%d".formatted(keyId));
        return verifierQueryRepository.findApiKeys(verifierId).stream()
                .filter(key -> key.keyId().equals(keyId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFIER_API_KEY_NOT_FOUND));
    }

    private Secret generateSecret() {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        String suffix = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String prefix = "kyvc_live_" + suffix.substring(0, 8);
        return new Secret(prefix, prefix + "." + suffix);
    }

    private void audit(String action, Long verifierId, String summary) {
        auditLogWriter.write(KyvcEnums.ActorType.ADMIN, SecurityUtil.getCurrentAdminId(), action,
                KyvcEnums.AuditTargetType.VERIFIER, verifierId, summary, null, null);
    }

    private record Secret(String prefix, String raw) {
    }
}
