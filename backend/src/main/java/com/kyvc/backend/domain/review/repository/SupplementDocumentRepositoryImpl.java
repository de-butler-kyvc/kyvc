package com.kyvc.backend.domain.review.repository;

import com.kyvc.backend.domain.review.domain.KycSupplementDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

// 보완요청 문서 Repository 구현체
@Repository
@RequiredArgsConstructor
public class SupplementDocumentRepositoryImpl implements SupplementDocumentRepository {

    private final SupplementDocumentJpaRepository supplementDocumentJpaRepository;

    // 보완요청 문서 매핑 저장
    @Override
    public KycSupplementDocument save(
            KycSupplementDocument supplementDocument // 저장 대상 문서 매핑
    ) {
        return supplementDocumentJpaRepository.save(supplementDocument);
    }

    // 보완요청 ID 기준 문서 매핑 목록 조회
    @Override
    public List<KycSupplementDocument> findBySupplementId(
            Long supplementId // 보완요청 ID
    ) {
        return supplementDocumentJpaRepository.findBySupplementIdOrderByCreatedAtDesc(supplementId);
    }

    // 보완요청 ID와 문서 ID 기준 매핑 존재 여부 조회
    @Override
    public boolean existsBySupplementIdAndDocumentId(
            Long supplementId, // 보완요청 ID
            Long documentId // 문서 ID
    ) {
        return supplementDocumentJpaRepository.existsBySupplementIdAndDocumentId(supplementId, documentId);
    }
}
