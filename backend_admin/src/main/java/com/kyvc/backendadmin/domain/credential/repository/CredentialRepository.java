package com.kyvc.backendadmin.domain.credential.repository;

import com.kyvc.backendadmin.global.util.KyvcEnums;

/**
 * Credential 저장 Repository입니다.
 */
public interface CredentialRepository {

    /**
     * 발급 중이거나 유효한 Credential 존재 여부를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return 존재하면 true
     */
    boolean existsIssuingOrValidByKycId(Long kycId);

    /**
     * credentials 테이블에 발급 대기 Credential row를 생성합니다.
     *
     * @param corporateId 법인 ID
     * @param kycId KYC 신청 ID
     * @param credentialExternalId 외부 Credential ID
     * @param credentialType Credential 유형
     * @param issuerDid Issuer DID
     * @param status Credential 상태
     * @return 생성된 Credential ID
     */
    Long createIssuing(
            Long corporateId,
            Long kycId,
            String credentialExternalId,
            KyvcEnums.CredentialType credentialType,
            String issuerDid,
            KyvcEnums.CredentialStatus status
    );
}
