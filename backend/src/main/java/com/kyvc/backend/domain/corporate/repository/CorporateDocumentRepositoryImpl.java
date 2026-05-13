package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.CorporateDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// 법인 문서 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CorporateDocumentRepositoryImpl implements CorporateDocumentRepository {

    private final CorporateDocumentJpaRepository corporateDocumentJpaRepository;

    // 법인 문서 저장
    @Override
    public CorporateDocument save(
            CorporateDocument corporateDocument // 저장 대상 법인 문서
    ) {
        return corporateDocumentJpaRepository.save(corporateDocument);
    }
}
