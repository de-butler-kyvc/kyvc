package com.kyvc.backendadmin.domain.document.repository;

import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestSearchRequest;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestSummaryResponse;

import java.util.List;

/**
 * 원본서류 삭제 요청 목록 검색 QueryRepository입니다.
 */
public interface DocumentDeleteRequestQueryRepository {

    /**
     * 원본서류 삭제 요청 목록을 검색합니다.
     *
     * @param request 검색 조건
     * @return 삭제 요청 목록
     */
    List<DocumentDeleteRequestSummaryResponse> search(DocumentDeleteRequestSearchRequest request);

    /**
     * 원본서류 삭제 요청 검색 결과 수를 조회합니다.
     *
     * @param request 검색 조건
     * @return 전체 건수
     */
    long count(DocumentDeleteRequestSearchRequest request);
}
