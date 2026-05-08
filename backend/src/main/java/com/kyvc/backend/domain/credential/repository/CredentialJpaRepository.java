package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Credential JPA Repository
 */
public interface CredentialJpaRepository extends JpaRepository<Credential, Long> {

    /**
     * KYC 요청 ID 기준 최신 Credential 조회
     *
     * @param kycId KYC 요청 ID
     * @return Credential 조회 결과
     */
    Optional<Credential> findFirstByKycIdOrderByCreatedAtDesc(
            Long kycId // KYC 요청 ID
    );

    /**
     * 법인 ID 기준 최신 Credential 조회
     *
     * @param corporateId 법인 ID
     * @return Credential 조회 결과
     */
    Optional<Credential> findFirstByCorporateIdOrderByCreatedAtDesc(
            Long corporateId // 법인 ID
    );

    /**
     * 법인 ID 기준 Credential 목록 조회
     *
     * @param corporateId 법인 ID
     * @return Credential 목록
     */
    List<Credential> findByCorporateIdOrderByCreatedAtDesc(
            Long corporateId // 법인 ID
    );

    /**
     * Credential ID 기준 Optional 조회
     *
     * @param credentialId Credential ID
     * @return Credential 조회 결과
     */
    Optional<Credential> findByCredentialId(
            Long credentialId // Credential ID
    );

    /**
     * 법인 ID + Wallet 저장 여부 기준 Credential 목록 조회
     *
     * @param corporateId 법인 ID
     * @param walletSavedYn Wallet 저장 여부
     * @return Credential 목록
     */
    List<Credential> findAllByCorporateIdAndWalletSavedYnOrderByWalletSavedAtDesc(
            Long corporateId, // 법인 ID
            String walletSavedYn // Wallet 저장 여부
    );

    /**
     * 법인 ID + Wallet 저장 여부 + Credential 상태 + 만료 일시 기준 Credential 목록 조회
     *
     * @param corporateId 법인 ID
     * @param walletSavedYn Wallet 저장 여부
     * @param credentialStatus Credential 상태
     * @param expiresAt 만료 기준 일시
     * @return Credential 목록
     */
    List<Credential> findAllByCorporateIdAndWalletSavedYnAndCredentialStatusAndExpiresAtGreaterThanEqualOrderByIssuedAtDesc(
            Long corporateId, // 법인 ID
            String walletSavedYn, // Wallet 저장 여부
            KyvcEnums.CredentialStatus credentialStatus, // Credential 상태
            LocalDateTime expiresAt // 만료 기준 일시
    );
}
