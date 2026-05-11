package com.kyvc.backend.domain.audit.repository;

import com.kyvc.backend.domain.corporate.dto.CorporateChangeHistoryResponse;

import java.util.List;

/**
 * 감사로그 조회 Repository
 */
public interface AuditLogQueryRepository {

    /**
     * 법인 대상 감사로그 목록 조회
     *
     * @param corporateId 법인 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 법인 변경 이력 목록
     */
    List<CorporateChangeHistoryResponse.Item> findCorporateHistories(
            Long corporateId, // 법인 ID
            int page, // 페이지 번호
            int size // 페이지 크기
    );

    /**
     * 법인 대상 감사로그 건수 조회
     *
     * @param corporateId 법인 ID
     * @return 감사로그 건수
     */
    long countCorporateHistories(
            Long corporateId // 법인 ID
    );
}
