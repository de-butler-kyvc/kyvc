package com.kyvc.backendadmin.domain.audit.repository;

import com.kyvc.backendadmin.domain.audit.domain.AuditLog;
import com.kyvc.backendadmin.domain.audit.dto.AdminAuditLogSearchRequest;

import java.util.List;

/**
 * 감사로그 검색 조회 책임을 가지는 QueryRepository입니다.
 */
public interface AuditLogQueryRepository {

    /**
     * actorType, actorId, actionType, targetType, targetId, from, to 기간 조건 기준으로 감사로그 목록을 검색합니다.
     *
     * @param request 감사로그 검색 조건
     * @return 감사로그 목록
     */
    List<AuditLog> search(AdminAuditLogSearchRequest request);

    /**
     * actorType, actorId, actionType, targetType, targetId, from, to 기간 조건 기준으로 감사로그 건수를 조회합니다.
     *
     * @param request 감사로그 검색 조건
     * @return 감사로그 건수
     */
    long count(AdminAuditLogSearchRequest request);
}
