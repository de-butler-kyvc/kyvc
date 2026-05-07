package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.Credential;

import java.util.List;
import java.util.Optional;

/**
 * Credential Repository
 */
public interface CredentialRepository {

    /**
     * KYC 요청 ID 기준 최신 Credential 조회
     *
     * @param kycId KYC 요청 ID
     * @return Credential 조회 결과
     */
    Optional<Credential> findLatestByKycId(
            Long kycId // KYC 요청 ID
    );

    /**
     * 법인 ID 기준 최신 Credential 조회
     *
     * @param corporateId 법인 ID
     * @return Credential 조회 결과
     */
    Optional<Credential> findLatestByCorporateId(
            Long corporateId // 법인 ID
    );

    /**
     * 법인 ID 기준 Credential 목록 조회
     *
     * @param corporateId 법인 ID
     * @return Credential 목록
     */
    List<Credential> findByCorporateId(
            Long corporateId // 법인 ID
    );

    /**
     * Credential ID 기준 Optional 조회
     *
     * @param credentialId Credential ID
     * @return Credential 조회 결과
     */
    Optional<Credential> findById(
            Long credentialId // Credential ID
    );

    /**
     * Credential ID 기준 조회
     *
     * @param credentialId Credential ID
     * @return Credential 조회 결과
     */
    /**
     * 법인 ID 기준 Wallet 저장 Credential 목록 조회
     *
     * @param corporateId 법인 ID
     * @return Wallet 저장 Credential 목록
     */
    List<Credential> findWalletCredentialsByCorporateId(
            Long corporateId // 법인 ID
    );

    Credential getById(
            Long credentialId // Credential ID
    );

    /**
     * Credential 저장
     *
     * @param credential 저장 대상 Credential
     * @return 저장된 Credential
     */
    Credential save(
            Credential credential // 저장 대상 Credential
    );
}
