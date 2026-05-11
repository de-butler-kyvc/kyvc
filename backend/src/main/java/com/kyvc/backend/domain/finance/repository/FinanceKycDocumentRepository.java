package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.document.domain.KycDocument;

import java.util.List;
import java.util.Optional;

/**
 * 금융사 방문 KYC 문서 Repository
 */
public interface FinanceKycDocumentRepository {

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
     * KYC 신청 ID 기준 목록 조회
     *
     * @param kycId KYC 신청 ID
     * @return 문서 목록
     */
    List<KycDocument> findByKycId(
            Long kycId // KYC 신청 ID
    );

    /**
     * 문서 저장
     *
     * @param kycDocument 저장 대상 문서
     * @return 저장된 문서
     */
    KycDocument save(
            KycDocument kycDocument // 저장 대상 문서
    );
}
