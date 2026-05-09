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

    /**
     * Credential 요청 이력을 저장합니다.
     *
     * @param credentialId Credential ID
     * @param requestTypeCode 요청 유형 코드
     * @param requestStatusCode 요청 상태 코드
     * @param requestedByTypeCode 요청자 유형 코드
     * @param requestedById 요청자 ID
     * @param reasonCode 사유 코드
     * @param reason 사유
     * @param coreRequestId Core 요청 ID
     * @return 생성된 요청 이력 ID
     */
    Long saveCredentialRequest(
            Long credentialId,
            String requestTypeCode,
            String requestStatusCode,
            String requestedByTypeCode,
            Long requestedById,
            String reasonCode,
            String reason,
            String coreRequestId
    );

    boolean existsInProgressCredentialRequest(Long credentialId, String requestTypeCode);

    /**
     * Credential 상태 변경 이력을 저장합니다.
     *
     * @param credentialId Credential ID
     * @param beforeStatusCode 변경 전 상태 코드
     * @param afterStatusCode 변경 후 상태 코드
     * @param changedByTypeCode 변경자 유형 코드
     * @param changedById 변경자 ID
     * @param reasonCode 사유 코드
     * @param reason 사유
     * @return 생성된 상태 이력 ID
     */
    Long saveStatusHistory(
            Long credentialId,
            String beforeStatusCode,
            String afterStatusCode,
            String changedByTypeCode,
            Long changedById,
            String reasonCode,
            String reason
    );
}
