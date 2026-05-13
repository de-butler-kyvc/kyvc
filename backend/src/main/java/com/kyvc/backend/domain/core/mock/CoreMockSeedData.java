package com.kyvc.backend.domain.core.mock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Core 연동 개발용 Seed 데이터
public final class CoreMockSeedData {

    private CoreMockSeedData() {
    }

    public static final String DEV_ISSUER_ACCOUNT = "rKYVCIssuerDev000000000000000000001";
    public static final String DEV_ISSUER_DID = "did:xrpl:1:" + DEV_ISSUER_ACCOUNT;
    public static final String DEV_ISSUER_VERIFICATION_METHOD_ID = DEV_ISSUER_DID + "#issuer-key-1";

    public static final String DEV_HOLDER_ACCOUNT = "rKYVCHolderDev000000000000000000001";
    public static final String DEV_HOLDER_DID = "did:xrpl:1:" + DEV_HOLDER_ACCOUNT;
    public static final String DEV_HOLDER_KEY_ID = "holder-test-key-1";
    public static final String DEV_HOLDER_DEVICE_ID = "DEV-HOLDER-DEVICE-001";

    // 개발 테스트용 holder seed, 운영 저장/로그/응답 금지
    public static final String DEV_HOLDER_SEED = "sEdKYVCHolderDevMockSeed000000000000000001";

    public static final String DEV_CREDENTIAL_TYPE = "KYC_CREDENTIAL";
    public static final String DEV_KYC_LEVEL = "BASIC";
    public static final String DEV_JURISDICTION = "KR";
    public static final String DEV_CREDENTIAL_STATUS_PURPOSE = "REVOCATION";

    public static final String DEV_VP_REQUEST_ID = "vp-req-dev-001";
    public static final String DEV_VP_NONCE = "holder-test-sdjwt-nonce-after-accept";
    public static final String DEV_VP_CHALLENGE = "holder-test-sdjwt-nonce-after-accept";
    public static final String DEV_VP_AUD = "https://dev-api-kyvc.khuoo.synology.me";

    // 법인 KYC Claim 기본값
    public static Map<String, Object> legalEntityClaims() {
        return Map.of(
                "kyc", Map.of(
                        "jurisdiction", DEV_JURISDICTION,
                        "assuranceLevel", "STANDARD"
                ),
                "legalEntity", Map.of(
                        "type", "STOCK_COMPANY",
                        "name", "KYvC Holder Test Co.",
                        "registrationNumber", "110111-7654321"
                ),
                "representative", Map.of(
                        "name", "Holder Tester",
                        "birthDate", "1980-01-01",
                        "nationality", "KR"
                ),
                "beneficialOwners", List.of(
                        Map.of(
                                "name", "Beneficial Owner",
                                "birthDate", "1975-02-03",
                                "nationality", "KR"
                        )
                )
        );
    }

    // VP 요청 필수 Claim 경로
    public static List<String> requiredClaims() {
        return List.of(
                "legalEntity.type",
                "representative.name",
                "representative.birthDate",
                "representative.nationality",
                "beneficialOwners[].name",
                "beneficialOwners[].birthDate",
                "beneficialOwners[].nationality"
        );
    }

    // VC 기본 만료 시각
    public static LocalDateTime validUntil() {
        return LocalDateTime.now().plusYears(1);
    }

    // VP 기본 만료 시각
    public static LocalDateTime vpExpiresAt() {
        return LocalDateTime.now().plusMinutes(5);
    }
}
