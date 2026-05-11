package com.kyvc.backend.domain.document.repository;

import com.kyvc.backend.domain.document.domain.DocumentDeleteRequest;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * 문서 삭제 요청 JPA Repository
 */
public interface DocumentDeleteRequestJpaRepository extends JpaRepository<DocumentDeleteRequest, Long> {

    /**
     * 문서 ID와 상태 목록 기준 존재 여부
     *
     * @param documentId 문서 ID
     * @param statuses 삭제 요청 상태 목록
     * @return 존재 여부
     */
    boolean existsByDocumentIdAndRequestStatusCodeIn(
            Long documentId, // 문서 ID
            Collection<KyvcEnums.DocumentDeleteRequestStatus> statuses // 삭제 요청 상태 목록
    );
}
