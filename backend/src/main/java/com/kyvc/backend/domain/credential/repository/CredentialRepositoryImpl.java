package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.Credential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Credential Repository 구현체
@Repository
@RequiredArgsConstructor
public class CredentialRepositoryImpl implements CredentialRepository {

    private final CredentialJpaRepository credentialJpaRepository;

    // KYC 요청 ID 기준 최신 Credential 조회
    @Override
    public Optional<Credential> findLatestByKycId(
            Long kycId // KYC 요청 ID
    ) {
        return credentialJpaRepository.findFirstByKycIdOrderByCreatedAtDesc(kycId);
    }

    // 법인 ID 기준 최신 Credential 조회
    @Override
    public Optional<Credential> findLatestByCorporateId(
            Long corporateId // 법인 ID
    ) {
        return credentialJpaRepository.findFirstByCorporateIdOrderByCreatedAtDesc(corporateId);
    }

    // 법인 ID 기준 Credential 목록 조회
    @Override
    public List<Credential> findByCorporateId(
            Long corporateId // 법인 ID
    ) {
        return credentialJpaRepository.findByCorporateIdOrderByCreatedAtDesc(corporateId);
    }
}
