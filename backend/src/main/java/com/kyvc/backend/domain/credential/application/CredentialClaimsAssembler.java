package com.kyvc.backend.domain.credential.application;

import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Credential claims 조립기
@Component
@RequiredArgsConstructor
public class CredentialClaimsAssembler {

    private final CorporateRepository corporateRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final CoreProperties coreProperties;
    private final LogEventLogger logEventLogger;

    // KYC 신청 기준 claims 조립
    public Map<String, Object> assemble(
            KycApplication kycApplication // KYC 신청
    ) {
        if (kycApplication == null || kycApplication.getKycId() == null) {
            throw new ApiException(ErrorCode.KYC_NOT_FOUND);
        }
        Corporate corporate = corporateRepository.findById(kycApplication.getCorporateId())
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        List<Map<String, Object>> documents = resolveDocumentClaims(kycApplication.getKycId());

        if (!isClaimsDataReady(kycApplication, corporate, documents)) {
            return fallbackClaimsIfEnabled(kycApplication.getKycId());
        }

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("legalEntity", legalEntityClaims(kycApplication, corporate));
        claims.put("kyc", kycClaims(kycApplication));
        claims.put("documents", documents);
        return claims;
    }

    private Map<String, Object> legalEntityClaims(
            KycApplication kycApplication, // KYC 신청
            Corporate corporate // 법인
    ) {
        Map<String, Object> legalEntity = new LinkedHashMap<>();
        legalEntity.put("name", corporate.getCorporateName());
        legalEntity.put("businessRegistrationNumber", corporate.getBusinessRegistrationNo());
        if (StringUtils.hasText(corporate.getCorporateRegistrationNo())) {
            legalEntity.put("corporateRegistrationNumber", corporate.getCorporateRegistrationNo());
        }
        legalEntity.put("type", kycApplication.getCorporateTypeCode());
        return legalEntity;
    }

    private Map<String, Object> kycClaims(
            KycApplication kycApplication // KYC 신청
    ) {
        Map<String, Object> kyc = new LinkedHashMap<>();
        kyc.put("status", KyvcEnums.KycStatus.APPROVED.name());
        kyc.put("assuranceLevel", KyvcEnums.KycLevel.STANDARD.name());
        kyc.put("jurisdiction", KyvcEnums.Jurisdiction.KR.name());
        if (kycApplication.getApprovedAt() != null) {
            kyc.put("approvedAt", kycApplication.getApprovedAt());
        }
        if (kycApplication.getApplicationChannelCode() != null) {
            kyc.put("applicationChannel", kycApplication.getApplicationChannelCode().name());
        }
        return kyc;
    }

    private List<Map<String, Object>> resolveDocumentClaims(
            Long kycId // KYC 신청 ID
    ) {
        return kycDocumentRepository.findByKycId(kycId).stream()
                .filter(document -> !document.isDeleted())
                .filter(document -> StringUtils.hasText(document.getDocumentTypeCode())
                        && StringUtils.hasText(document.getDocumentHash()))
                .map(this::toDocumentClaim)
                .toList();
    }

    private Map<String, Object> toDocumentClaim(
            KycDocument document // KYC 문서
    ) {
        Map<String, Object> documentClaim = new LinkedHashMap<>();
        documentClaim.put("type", document.getDocumentTypeCode());
        documentClaim.put("hash", document.getDocumentHash());
        return documentClaim;
    }

    private boolean isClaimsDataReady(
            KycApplication kycApplication, // KYC 신청
            Corporate corporate, // 법인
            List<Map<String, Object>> documents // 문서 claims
    ) {
        return KyvcEnums.KycStatus.APPROVED == kycApplication.getKycStatus()
                && StringUtils.hasText(corporate.getCorporateName())
                && StringUtils.hasText(corporate.getBusinessRegistrationNo())
                && StringUtils.hasText(kycApplication.getCorporateTypeCode())
                && !documents.isEmpty();
    }

    private Map<String, Object> fallbackClaimsIfEnabled(
            Long kycId // KYC 신청 ID
    ) {
        if (!coreProperties.isDevSeedEnabled()) {
            throw new ApiException(ErrorCode.CREDENTIAL_CLAIMS_REQUIRED_DATA_MISSING);
        }
        logEventLogger.warn(
                "credential.claims.dev_seed_used",
                "Credential claims 개발 seed 사용",
                Map.of("kycId", kycId, "devSeedUsed", true)
        );
        return CoreMockSeedData.legalEntityClaims();
    }
}
