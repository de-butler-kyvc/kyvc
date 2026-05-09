package com.kyvc.backendadmin.domain.security.repository;

import com.kyvc.backendadmin.domain.security.dto.AdminSecurityDtos;

import java.util.List;

/**
 * 보안 이벤트 조회 QueryRepository입니다.
 */
public interface SecurityEventQueryRepository {
    List<AdminSecurityDtos.EventResponse> search(boolean dataAccess, int page, int size);
    long count(boolean dataAccess);
}
