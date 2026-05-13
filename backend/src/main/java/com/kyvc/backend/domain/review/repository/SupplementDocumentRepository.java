package com.kyvc.backend.domain.review.repository;

import com.kyvc.backend.domain.review.domain.KycSupplementDocument;

import java.util.List;

/**
 * 보완요청 문서 Repository
 */
public interface SupplementDocumentRepository {

    /**
     * 보완요청 문서 매핑 저장
     *
     * @param supplementDocument 저장 대상 문서 매핑
     * @return 저장된 문서 매핑
     */
    KycSupplementDocument save(
            KycSupplementDocument supplementDocument // 저장 대상 문서 매핑
    );

    /**
     * 보완요청 ID 기준 문서 매핑 목록 조회
     *
     * @param supplementId 보완요청 ID
     * @return 문서 매핑 목록
     */
    List<KycSupplementDocument> findBySupplementId(
            Long supplementId // 보완요청 ID
    );

    /**
     * 보완요청 ID와 문서 ID 기준 매핑 존재 여부 조회
     *
     * @param supplementId 보완요청 ID
     * @param documentId 문서 ID
     * @return 매핑 존재 여부
     */
    boolean existsBySupplementIdAndDocumentId(
            Long supplementId, // 보완요청 ID
            Long documentId // 문서 ID
    );
}
