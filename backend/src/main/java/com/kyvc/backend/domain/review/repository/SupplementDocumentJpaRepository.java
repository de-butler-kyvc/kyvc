package com.kyvc.backend.domain.review.repository;

import com.kyvc.backend.domain.review.domain.KycSupplementDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 보완요청 문서 JPA Repository
 */
public interface SupplementDocumentJpaRepository extends JpaRepository<KycSupplementDocument, Long> {

    /**
     * 보완요청 ID 기준 문서 매핑 목록 조회
     *
     * @param supplementId 보완요청 ID
     * @return 문서 매핑 목록
     */
    List<KycSupplementDocument> findBySupplementIdOrderByCreatedAtDesc(
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
