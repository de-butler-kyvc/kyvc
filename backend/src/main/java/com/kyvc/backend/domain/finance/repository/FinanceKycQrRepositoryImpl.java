package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 금융사 방문 KYC QR Repository 구현체
@Repository
@RequiredArgsConstructor
public class FinanceKycQrRepositoryImpl implements FinanceKycQrRepository {

    private final CredentialRepository credentialRepository;

    // KYC 신청 ID 기준 최신 Credential 조회
    @Override
    public Optional<Credential> findLatestByKycId(
            Long kycId // KYC 신청 ID
    ) {
        return credentialRepository.findLatestByKycId(kycId);
    }

    // Credential 저장
    @Override
    public Credential save(
            Credential credential // 저장 대상 Credential
    ) {
        return credentialRepository.save(credential);
    }
}
