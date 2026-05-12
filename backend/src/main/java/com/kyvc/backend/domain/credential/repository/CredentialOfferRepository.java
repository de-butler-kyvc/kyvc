package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialOffer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Credential Offer Repository
 */
public interface CredentialOfferRepository {

    /**
     * Credential Offer 저장
     *
     * @param offer 저장 대상 Offer
     * @return 저장된 Offer
     */
    CredentialOffer save(
            CredentialOffer offer // 저장 대상 Offer
    );

    /**
     * Credential Offer ID 기준 Optional 조회
     *
     * @param credentialOfferId Credential Offer ID
     * @return Credential Offer 조회 결과
     */
    Optional<CredentialOffer> findById(
            Long credentialOfferId // Credential Offer ID
    );

    /**
     * Credential Offer ID 기준 필수 조회
     *
     * @param credentialOfferId Credential Offer ID
     * @return Credential Offer
     */
    CredentialOffer getById(
            Long credentialOfferId // Credential Offer ID
    );

    /**
     * KYC 신청 ID 기준 최신 활성 Offer 조회
     *
     * @param kycId KYC 신청 ID
     * @param now 기준 일시
     * @return 최신 활성 Offer 조회 결과
     */
    Optional<CredentialOffer> findLatestActiveByKycId(
            Long kycId, // KYC 신청 ID
            LocalDateTime now // 기준 일시
    );

    /**
     * KYC 신청 ID 기준 활성 Offer 목록 조회
     *
     * @param kycId KYC 신청 ID
     * @return 활성 Offer 목록
     */
    List<CredentialOffer> findActiveOffersByKycId(
            Long kycId // KYC 신청 ID
    );

    /**
     * KYC 신청 ID 기준 최신 Offer 조회
     *
     * @param kycId KYC 신청 ID
     * @return 최신 Offer 조회 결과
     */
    Optional<CredentialOffer> findLatestByKycId(
            Long kycId // KYC 신청 ID
    );

    /**
     * KYC 신청 ID 기준 활성 Offer 존재 여부
     *
     * @param kycId KYC 신청 ID
     * @param now 기준 일시
     * @return 활성 Offer 존재 여부
     */
    boolean existsActiveByKycId(
            Long kycId, // KYC 신청 ID
            LocalDateTime now // 기준 일시
    );
}
