package com.kyvc.backend.domain.issuer.repository;

import com.kyvc.backend.domain.issuer.domain.IssuerConfig;

import java.util.Optional;

/**
 * Issuer 설정 Repository
 */
public interface IssuerConfigRepository {

    /**
     * Credential 유형 기준 기본 활성 Issuer 설정 조회
     *
     * @param credentialTypeCode Credential 유형 코드
     * @return Issuer 설정 조회 결과
     */
    Optional<IssuerConfig> findDefaultActiveByCredentialType(
            String credentialTypeCode // Credential 유형 코드
    );
}
