package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialOffer;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Credential Offer Repository 구현체
@Repository
@RequiredArgsConstructor
public class CredentialOfferRepositoryImpl implements CredentialOfferRepository {

    private final CredentialOfferJpaRepository credentialOfferJpaRepository;

    // Credential Offer 저장
    @Override
    public CredentialOffer save(
            CredentialOffer offer // 저장 대상 Offer
    ) {
        return credentialOfferJpaRepository.save(offer);
    }

    // Credential Offer ID 기준 Optional 조회
    @Override
    public Optional<CredentialOffer> findById(
            Long credentialOfferId // Credential Offer ID
    ) {
        return credentialOfferJpaRepository.findByCredentialOfferId(credentialOfferId);
    }

    // Credential Offer ID 기준 필수 조회
    @Override
    public CredentialOffer getById(
            Long credentialOfferId // Credential Offer ID
    ) {
        return findById(credentialOfferId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_OFFER_NOT_FOUND));
    }

    // KYC 신청 ID 기준 최신 활성 Offer 조회
    @Override
    public Optional<CredentialOffer> findLatestActiveByKycId(
            Long kycId, // KYC 신청 ID
            LocalDateTime now // 기준 일시
    ) {
        return credentialOfferJpaRepository.findFirstByKycIdAndOfferStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                kycId,
                KyvcEnums.CredentialOfferStatus.ACTIVE,
                now
        );
    }

    // KYC 신청 ID 기준 활성 Offer 목록 조회
    @Override
    public List<CredentialOffer> findActiveOffersByKycId(
            Long kycId // KYC 신청 ID
    ) {
        return credentialOfferJpaRepository.findByKycIdAndOfferStatusOrderByCreatedAtDesc(
                kycId,
                KyvcEnums.CredentialOfferStatus.ACTIVE
        );
    }

    // KYC 신청 ID 기준 최신 Offer 조회
    @Override
    public Optional<CredentialOffer> findLatestByKycId(
            Long kycId // KYC 신청 ID
    ) {
        return credentialOfferJpaRepository.findFirstByKycIdOrderByCreatedAtDesc(kycId);
    }

    // KYC 신청 ID 기준 활성 Offer 존재 여부
    @Override
    public boolean existsActiveByKycId(
            Long kycId, // KYC 신청 ID
            LocalDateTime now // 기준 일시
    ) {
        return credentialOfferJpaRepository.existsByKycIdAndOfferStatusAndExpiresAtAfter(
                kycId,
                KyvcEnums.CredentialOfferStatus.ACTIVE,
                now
        );
    }
}
