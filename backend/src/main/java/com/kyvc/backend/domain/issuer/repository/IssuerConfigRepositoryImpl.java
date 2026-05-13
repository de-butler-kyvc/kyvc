package com.kyvc.backend.domain.issuer.repository;

import com.kyvc.backend.domain.issuer.domain.IssuerConfig;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Issuer 설정 Repository 구현체
@Repository
@RequiredArgsConstructor
public class IssuerConfigRepositoryImpl implements IssuerConfigRepository {

    private final IssuerConfigJpaRepository issuerConfigJpaRepository;

    // Credential 유형 기준 기본 활성 Issuer 설정 조회
    @Override
    public Optional<IssuerConfig> findDefaultActiveByCredentialType(
            String credentialTypeCode // Credential 유형 코드
    ) {
        return issuerConfigJpaRepository.findFirstByCredentialTypeCodeAndDefaultYnAndIssuerConfigStatusOrderByIssuerConfigIdDesc(
                credentialTypeCode,
                KyvcEnums.Yn.Y.name(),
                KyvcEnums.IssuerConfigStatus.ACTIVE
        );
    }
}
