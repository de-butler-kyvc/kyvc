package com.kyvc.backend.domain.document.repository;

import com.kyvc.backend.domain.document.domain.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * KYC 문서 JPA Repository
 */
public interface KycDocumentJpaRepository extends JpaRepository<KycDocument, Long> {

    /**
     * KYC 신청 ID 기준 문서 목록 조회
     *
     * @param kycId KYC 신청 ID
     * @return 문서 목록
     */
    List<KycDocument> findByKycIdOrderByUploadedAtDesc(
            Long kycId // KYC 신청 ID
    );

    /**
     * KYC 신청 ID와 문서 유형 기준 문서 목록 조회
     *
     * @param kycId KYC 신청 ID
     * @param documentTypeCode 문서 유형 코드
     * @return 문서 목록
     */
    List<KycDocument> findByKycIdAndDocumentTypeCodeOrderByUploadedAtDesc(
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
}
