package com.kyvc.backend.domain.credential.application;

import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.domain.CorporateAgent;
import com.kyvc.backend.domain.corporate.domain.CorporateRepresentative;
import com.kyvc.backend.domain.corporate.repository.CorporateAgentRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepresentativeRepository;
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

import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Credential claims 조립기
@Component
@RequiredArgsConstructor
public class CredentialClaimsAssembler {

    private final CorporateRepository corporateRepository;
    private final CorporateRepresentativeRepository corporateRepresentativeRepository;
    private final CorporateAgentRepository corporateAgentRepository;
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
        List<Map<String, Object>> documentEvidence = resolveDocumentEvidenceClaims(kycApplication.getKycId());

        if (!isClaimsDataReady(kycApplication, corporate, documentEvidence)) {
            return fallbackClaimsIfEnabled(kycApplication.getKycId());
        }

        Map<String, Object> claims = new LinkedHashMap<>();
        putIfNotEmpty(claims, "kyc", kycClaims(kycApplication));
        putIfNotEmpty(claims, "legalEntity", legalEntityClaims(kycApplication, corporate));
        putIfNotEmpty(claims, "representative", representativeClaims(corporate));
        putIfNotEmpty(claims, "delegate", delegateClaims(corporate));
        putIfNotEmpty(claims, "delegation", delegationClaims(corporate));
        putIfNotEmpty(claims, "extra", extraClaims(kycApplication, corporate));
        claims.put("documentEvidence", documentEvidence);
        return claims;
    }

    private Map<String, Object> legalEntityClaims(
            KycApplication kycApplication, // KYC 신청
            Corporate corporate // 법인
    ) {
        Map<String, Object> legalEntity = new LinkedHashMap<>();
        putIfPresent(legalEntity, "name", corporate.getCorporateName());
        putIfPresent(legalEntity, "registrationNumber", corporate.getBusinessRegistrationNo());
        putIfPresent(legalEntity, "corporateRegistrationNumber", corporate.getCorporateRegistrationNo());
        putIfPresent(legalEntity, "type", firstText(kycApplication.getCorporateTypeCode(), corporate.getCorporateTypeCode()));
        putIfPresent(legalEntity, "establishedDate", corporate.getEstablishedDate());
        putIfPresent(legalEntity, "address", corporate.getAddress());
        putIfPresent(legalEntity, "businessType", corporate.getBusinessType());
        return legalEntity;
    }

    private Map<String, Object> kycClaims(
            KycApplication kycApplication // KYC 신청
    ) {
        Map<String, Object> kyc = new LinkedHashMap<>();
        putIfPresent(kyc, "status", kycApplication.getKycStatus());
        putIfPresent(kyc, "jurisdiction", KyvcEnums.Jurisdiction.KR);
        putIfPresent(kyc, "assuranceLevel", KyvcEnums.KycLevel.STANDARD);
        putIfPresent(kyc, "approvedAt", kycApplication.getApprovedAt());
        putIfPresent(kyc, "applicationChannel", kycApplication.getApplicationChannelCode());
        return kyc;
    }

    private Map<String, Object> representativeClaims(
            Corporate corporate
    ) {
        Map<String, Object> representative = new LinkedHashMap<>();
        Optional<CorporateRepresentative> activeRepresentative = corporateRepresentativeRepository.findByCorporateId(corporate.getCorporateId())
                .filter(this::isActiveRepresentative);

        activeRepresentative.ifPresent(value -> {
            putIfPresent(representative, "name", value.getRepresentativeName());
            putIfPresent(representative, "birthDate", value.getBirthDate());
            putIfPresent(representative, "nationality", value.getNationalityCode());
            putIfPresent(representative, "phone", value.getPhone());
            putIfPresent(representative, "email", value.getEmail());
        });
        putIfAbsentPresent(representative, "name", corporate.getRepresentativeName());
        putIfAbsentPresent(representative, "phone", corporate.getRepresentativePhone());
        putIfAbsentPresent(representative, "email", corporate.getRepresentativeEmail());
        return representative;
    }

    private Map<String, Object> delegateClaims(
            Corporate corporate
    ) {
        Map<String, Object> delegate = new LinkedHashMap<>();
        activeCorporateAgent(corporate.getCorporateId()).ifPresent(value -> {
            putIfPresent(delegate, "name", value.getAgentName());
            putIfPresent(delegate, "birthDate", value.getAgentBirthDate());
            putIfPresent(delegate, "phone", value.getAgentPhone());
            putIfPresent(delegate, "email", value.getAgentEmail());
        });
        putIfAbsentPresent(delegate, "name", corporate.getAgentName());
        putIfAbsentPresent(delegate, "phone", corporate.getAgentPhone());
        putIfAbsentPresent(delegate, "email", corporate.getAgentEmail());
        return delegate;
    }

    private Map<String, Object> delegationClaims(
            Corporate corporate
    ) {
        Map<String, Object> delegation = new LinkedHashMap<>();
        activeCorporateAgent(corporate.getCorporateId()).ifPresent(value -> {
            putIfPresent(delegation, "authorityScope", value.getAuthorityScope());
            putIfPresent(delegation, "status", value.getAuthorityStatusCode());
            putIfPresent(delegation, "validFrom", value.getValidFrom());
            putIfPresent(delegation, "validUntil", value.getValidTo());
        });
        putIfAbsentPresent(delegation, "authorityScope", corporate.getAgentAuthorityScope());
        return delegation;
    }

    private Map<String, Object> extraClaims(
            KycApplication kycApplication,
            Corporate corporate
    ) {
        Map<String, Object> extra = new LinkedHashMap<>();
        putIfPresent(extra, "corporateRegistrationNumber", corporate.getCorporateRegistrationNo());
        putIfPresent(extra, "aiReviewStatus", kycApplication.getAiReviewStatus());
        putIfPresent(extra, "aiReviewResult", kycApplication.getAiReviewResult());
        putIfPresent(extra, "aiConfidenceScore", kycApplication.getAiConfidenceScore());
        putIfPresent(extra, "aiReviewSummary", kycApplication.getAiReviewSummary());
        return extra;
    }

    private Optional<CorporateAgent> activeCorporateAgent(
            Long corporateId // 踰뺤씤 ID
    ) {
        return corporateAgentRepository.findByCorporateId(corporateId).stream()
                .filter(agent -> KyvcEnums.AgentAuthorityStatus.ACTIVE == agent.getAuthorityStatusCode())
                .findFirst();
    }

    private boolean isActiveRepresentative(
            CorporateRepresentative representative // 踰뺤씤 ??쒖옄
    ) {
        return KyvcEnums.Yn.Y == representative.getActiveYn();
    }

    private List<Map<String, Object>> resolveDocumentEvidenceClaims(
            Long kycId // KYC 신청 ID
    ) {
        return kycDocumentRepository.findByKycId(kycId).stream()
                .filter(document -> !document.isDeleted())
                .filter(document -> StringUtils.hasText(document.getDocumentTypeCode())
                        && StringUtils.hasText(document.getDocumentHash()))
                .map(this::toDocumentEvidenceClaim)
                .toList();
    }

    private Map<String, Object> toDocumentEvidenceClaim(
            KycDocument document // KYC 문서
    ) {
        Map<String, Object> documentClaim = new LinkedHashMap<>();
        if (document.getDocumentId() != null) {
            documentClaim.put("documentId", "urn:kyvc:doc:" + document.getDocumentId());
        }
        putIfPresent(documentClaim, "documentType", document.getDocumentTypeCode());
        putIfPresent(documentClaim, "documentClass", document.getDocumentTypeCode());
        putIfPresent(documentClaim, "digestSRI", document.getDocumentHash());
        putIfPresent(documentClaim, "mediaType", document.getMimeType());
        putIfPresent(documentClaim, "byteSize", document.getFileSize());
        putIfPresent(documentClaim, "uploadedAt", document.getUploadedAt());
        return documentClaim;
    }

    private boolean isClaimsDataReady(
            KycApplication kycApplication, // KYC 신청
            Corporate corporate, // 법인
            List<Map<String, Object>> documentEvidence // 문서 증빙 claims
    ) {
        return KyvcEnums.KycStatus.APPROVED == kycApplication.getKycStatus()
                && StringUtils.hasText(corporate.getCorporateName())
                && StringUtils.hasText(corporate.getBusinessRegistrationNo())
                && StringUtils.hasText(kycApplication.getCorporateTypeCode())
                && !documentEvidence.isEmpty();
    }

    private void putIfPresent(
            Map<String, Object> target, // claims Map
            String key, // claims key
            Object value // claims value
    ) {
        Object normalized = normalizeValue(value);
        if (normalized != null) {
            target.put(key, normalized);
        }
    }

    private void putIfAbsentPresent(
            Map<String, Object> target, // claims Map
            String key, // claims key
            Object value // claims value
    ) {
        if (!target.containsKey(key)) {
            putIfPresent(target, key, value);
        }
    }

    private void putIfNotEmpty(
            Map<String, Object> target, // claims Map
            String key, // claims key
            Map<String, Object> value // claims value
    ) {
        if (!value.isEmpty()) {
            target.put(key, value);
        }
    }

    private Object normalizeValue(
            Object value // claims value
    ) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return StringUtils.hasText(stringValue) ? stringValue : null;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof TemporalAccessor) {
            return value.toString();
        }
        return value;
    }

    private String firstText(
            String first,
            String second
    ) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return second;
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
