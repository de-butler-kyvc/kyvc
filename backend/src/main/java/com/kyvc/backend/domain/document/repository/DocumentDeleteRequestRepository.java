package com.kyvc.backend.domain.document.repository;

import com.kyvc.backend.domain.document.domain.DocumentDeleteRequest;
import com.kyvc.backend.global.util.KyvcEnums;

import java.util.Collection;

/**
 * 문서 삭제 요청 Repository
 */
public interface DocumentDeleteRequestRepository {

    /**
     * 문서 삭제 요청 저장
     *
     * @param request 저장 대상 삭제 요청
     * @return 저장된 삭제 요청
     */
    DocumentDeleteRequest save(
            DocumentDeleteRequest request // 저장 대상 삭제 요청
    );

    /**
     * 문서 기준 삭제 요청 상태 존재 여부
     *
     * @param documentId 문서 ID
     * @param statuses 삭제 요청 상태 목록
     * @return 존재 여부
     */
    boolean existsByDocumentIdAndStatuses(
            Long documentId, // 문서 ID
            Collection<KyvcEnums.DocumentDeleteRequestStatus> statuses // 삭제 요청 상태 목록
    );
}
