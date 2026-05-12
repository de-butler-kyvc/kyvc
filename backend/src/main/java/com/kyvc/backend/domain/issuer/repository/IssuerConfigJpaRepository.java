package com.kyvc.backend.domain.issuer.repository;

import com.kyvc.backend.domain.issuer.domain.IssuerConfig;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Issuer 설정 JPA Repository
 */
public interface IssuerConfigJpaRepository extends JpaRepository<IssuerConfig, Long> {

    /**
     * Credential 유형 + 기본 여부 + 상태 기준 최신 Issuer 설정 조회
     *
     * @param credentialTypeCode Credential 유형 코드
     * @param defaultYn 기본 여부
     * @param issuerConfigStatus Issuer 설정 상태
     * @return Issuer 설정 조회 결과
     */
    Optional<IssuerConfig> findFirstByCredentialTypeCodeAndDefaultYnAndIssuerConfigStatusOrderByIssuerConfigIdDesc(
            String credentialTypeCode, // Credential 유형 코드
            String defaultYn, // 기본 여부
            KyvcEnums.IssuerConfigStatus issuerConfigStatus // Issuer 설정 상태
    );
}
