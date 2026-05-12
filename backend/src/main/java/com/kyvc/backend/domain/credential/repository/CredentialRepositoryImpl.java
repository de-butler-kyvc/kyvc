package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // KYC 신청 ID 기준 Wallet 저장 완료 유효 Credential 존재 여부
    @Override
    public boolean existsWalletSavedValidByKycId(
            Long kycId // KYC 신청 ID
    ) {
        return credentialJpaRepository.existsByKycIdAndWalletSavedYnAndCredentialStatusAndExpiresAtGreaterThanEqual(
                kycId,
                KyvcEnums.Yn.Y.name(),
                KyvcEnums.CredentialStatus.VALID,
                LocalDateTime.now()
        );
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

    // Credential ID 기준 Optional 조회
    @Override
    public Optional<Credential> findById(
            Long credentialId // Credential ID
    ) {
        return credentialJpaRepository.findByCredentialId(credentialId);
    }

    // Credential ID 기준 조회
    @Override
    public Credential getById(
            Long credentialId // Credential ID
    ) {
        return findById(credentialId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_NOT_FOUND));
    }

    // Credential 저장
    @Override
    public Credential save(
            Credential credential // 저장 대상 Credential
    ) {
        return credentialJpaRepository.save(credential);
    }

    // Wallet 저장 Credential 목록 조회
    @Override
    public List<Credential> findWalletCredentialsByCorporateId(
            Long corporateId // 법인 ID
    ) {
        return credentialJpaRepository.findAllByCorporateIdAndWalletSavedYnOrderByWalletSavedAtDesc(
                corporateId,
                KyvcEnums.Yn.Y.name()
        );
    }

    // VP 제출 가능 Credential 목록 조회
    @Override
    public List<Credential> findVpEligibleCredentialsByCorporateId(
            Long corporateId // 법인 ID
    ) {
        return credentialJpaRepository
                .findAllByCorporateIdAndWalletSavedYnAndCredentialStatusAndExpiresAtGreaterThanEqualOrderByIssuedAtDesc(
                        corporateId,
                        KyvcEnums.Yn.Y.name(),
                        KyvcEnums.CredentialStatus.VALID,
                        LocalDateTime.now()
                );
    }
    // 테스트 검증용 유효 Credential 최신 조회
    @Override
    public Optional<Credential> findLatestValid() {
        return credentialJpaRepository
                .findFirstByCredentialStatusAndExpiresAtGreaterThanEqualOrderByIssuedAtDesc(
                        KyvcEnums.CredentialStatus.VALID,
                        LocalDateTime.now()
                );
    }
}
