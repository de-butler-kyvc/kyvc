package com.kyvc.backend.domain.document.repository;

import com.kyvc.backend.domain.document.domain.DocumentDeleteRequest;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;

// 문서 삭제 요청 Repository 구현체
@Repository
@RequiredArgsConstructor
public class DocumentDeleteRequestRepositoryImpl implements DocumentDeleteRequestRepository {

    private final DocumentDeleteRequestJpaRepository documentDeleteRequestJpaRepository;

    // 문서 삭제 요청 저장
    @Override
    public DocumentDeleteRequest save(
            DocumentDeleteRequest request // 저장 대상 삭제 요청
    ) {
        return documentDeleteRequestJpaRepository.save(request);
    }

    // 문서 기준 삭제 요청 상태 존재 여부
    @Override
    public boolean existsByDocumentIdAndStatuses(
            Long documentId, // 문서 ID
            Collection<KyvcEnums.DocumentDeleteRequestStatus> statuses // 삭제 요청 상태 목록
    ) {
        return documentDeleteRequestJpaRepository.existsByDocumentIdAndRequestStatusCodeIn(documentId, statuses);
    }
}
