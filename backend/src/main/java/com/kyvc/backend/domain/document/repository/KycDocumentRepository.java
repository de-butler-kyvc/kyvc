package com.kyvc.backend.domain.document.repository;

import com.kyvc.backend.domain.document.domain.KycDocument;

import java.util.List;
import java.util.Optional;

/**
 * KYC 문서 Repository
 */
public interface KycDocumentRepository {

    /**
     * 문서 ID 기준 조회
     *
     * @param documentId 문서 ID
     * @return 문서 조회 결과
     */
    Optional<KycDocument> findById(
            Long documentId // 문서 ID
    );

    /**
     * KYC 신청 ID 기준 문서 목록 조회
     *
     * @param kycId KYC 신청 ID
     * @return 문서 목록
     */
    List<KycDocument> findByKycId(
            Long kycId // KYC 신청 ID
    );

    /**
     * KYC 신청 ID와 문서 유형 기준 문서 목록 조회
     *
     * @param kycId KYC 신청 ID
     * @param documentTypeCode 문서 유형 코드
     * @return 문서 목록
     */
    List<KycDocument> findByKycIdAndDocumentTypeCode(
            Long kycId, // KYC 신청 ID
            String documentTypeCode // 문서 유형 코드
    );

    /**
     * KYC 신청 ID와 문서 유형 기준 문서 존재 여부
     *
     * @param kycId KYC 신청 ID
     * @param documentTypeCode 문서 유형 코드
     * @return 문서 존재 여부
     */
    boolean existsByKycIdAndDocumentTypeCode(
            Long kycId, // KYC 신청 ID
            String documentTypeCode // 문서 유형 코드
    );

    /**
     * KYC 문서 저장
     *
     * @param kycDocument 저장 대상 KYC 문서
     * @return 저장된 KYC 문서
     */
    KycDocument save(
            KycDocument kycDocument // 저장 대상 KYC 문서
    );
}
