package com.kyvc.backend.domain.credential.application;

import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.domain.issuer.domain.IssuerConfig;
import com.kyvc.backend.domain.issuer.repository.IssuerConfigRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

// Credential 발급 Issuer 해석기
@Component
@RequiredArgsConstructor
public class CredentialIssuerResolver {

    private static final String DEFAULT_SIGNING_KEY_REF = "issuer-key-1";

    private final IssuerConfigRepository issuerConfigRepository;
    private final CoreProperties coreProperties;
    private final LogEventLogger logEventLogger;

    // KYC Credential 발급 Issuer 해석
    public ResolvedIssuer resolveKycIssuer() {
        String credentialType = KyvcEnums.CredentialType.KYC_CREDENTIAL.name(); // Credential 유형
        return issuerConfigRepository.findDefaultActiveByCredentialType(credentialType)
                .map(this::toResolvedIssuer)
                .orElseGet(() -> resolveDevSeedIssuer(credentialType));
    }

    private ResolvedIssuer toResolvedIssuer(
            IssuerConfig issuerConfig // Issuer 설정
    ) {
        if (issuerConfig == null
                || !issuerConfig.isActive()
                || !StringUtils.hasText(issuerConfig.getIssuerXrplAddress())
                || !StringUtils.hasText(issuerConfig.getIssuerDid())
                || !StringUtils.hasText(issuerConfig.getVerificationMethodId())
                || !StringUtils.hasText(issuerConfig.getSigningKeyRef())
                || !StringUtils.hasText(issuerConfig.getCredentialTypeCode())) {
            throw new ApiException(ErrorCode.CREDENTIAL_ISSUER_CONFIG_NOT_FOUND);
        }
        return new ResolvedIssuer(
                issuerConfig.getIssuerXrplAddress().trim(),
                issuerConfig.getIssuerDid().trim(),
                issuerConfig.getVerificationMethodId().trim(),
                issuerConfig.getSigningKeyRef().trim(),
                issuerConfig.getCredentialTypeCode().trim()
        );
    }

    private ResolvedIssuer resolveDevSeedIssuer(
            String credentialType // Credential 유형
    ) {
        if (!coreProperties.isDevSeedEnabled()) {
            throw new ApiException(ErrorCode.CREDENTIAL_ISSUER_CONFIG_NOT_FOUND);
        }
        logEventLogger.warn(
                "credential.issuer.dev_seed_used",
                "Credential Issuer 개발 seed 사용",
                createDevSeedLogFields(credentialType)
        );
        return new ResolvedIssuer(
                CoreMockSeedData.DEV_ISSUER_ACCOUNT,
                CoreMockSeedData.DEV_ISSUER_DID,
                CoreMockSeedData.DEV_ISSUER_VERIFICATION_METHOD_ID,
                DEFAULT_SIGNING_KEY_REF,
                credentialType
        );
    }

    private Map<String, Object> createDevSeedLogFields(
            String credentialType // Credential 유형
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("credentialType", credentialType);
        fields.put("devSeedUsed", true);
        return fields;
    }
}
