package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialOffer;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Credential Offer JPA Repository
 */
public interface CredentialOfferJpaRepository extends JpaRepository<CredentialOffer, Long> {

    /**
     * Credential Offer ID 기준 Optional 조회
     *
     * @param credentialOfferId Credential Offer ID
     * @return Credential Offer 조회 결과
     */
    Optional<CredentialOffer> findByCredentialOfferId(
            Long credentialOfferId // Credential Offer ID
    );

    /**
     * KYC 신청 ID와 상태 기준 최신 Offer 조회
     *
     * @param kycId KYC 신청 ID
     * @param offerStatus Offer 상태
     * @param expiresAt 만료 기준 일시
     * @return 최신 활성 Offer 조회 결과
     */
    Optional<CredentialOffer> findFirstByKycIdAndOfferStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Long kycId, // KYC 신청 ID
            KyvcEnums.CredentialOfferStatus offerStatus, // Offer 상태
            LocalDateTime expiresAt // 만료 기준 일시
    );

    /**
     * KYC 신청 ID와 상태 기준 Offer 목록 조회
     *
     * @param kycId KYC 신청 ID
     * @param offerStatus Offer 상태
     * @return Offer 목록
     */
    List<CredentialOffer> findByKycIdAndOfferStatusOrderByCreatedAtDesc(
            Long kycId, // KYC 신청 ID
            KyvcEnums.CredentialOfferStatus offerStatus // Offer 상태
    );

    /**
     * KYC 신청 ID 기준 최신 Offer 조회
     *
     * @param kycId KYC 신청 ID
     * @return 최신 Offer 조회 결과
     */
    Optional<CredentialOffer> findFirstByKycIdOrderByCreatedAtDesc(
            Long kycId // KYC 신청 ID
    );

    /**
     * KYC 신청 ID와 상태 기준 활성 Offer 존재 여부
     *
     * @param kycId KYC 신청 ID
     * @param offerStatus Offer 상태
     * @param expiresAt 만료 기준 일시
     * @return 활성 Offer 존재 여부
     */
    boolean existsByKycIdAndOfferStatusAndExpiresAtAfter(
            Long kycId, // KYC 신청 ID
            KyvcEnums.CredentialOfferStatus offerStatus, // Offer 상태
            LocalDateTime expiresAt // 만료 기준 일시
    );
}
