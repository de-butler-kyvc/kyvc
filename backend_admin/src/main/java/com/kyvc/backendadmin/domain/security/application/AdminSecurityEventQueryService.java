package com.kyvc.backendadmin.domain.security.application;

import com.kyvc.backendadmin.domain.security.dto.AdminSecurityDtos;
import com.kyvc.backendadmin.domain.security.repository.SecurityEventQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보안 이벤트 조회 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminSecurityEventQueryService {
    private final SecurityEventQueryRepository repository;

    /** 보안 이벤트를 조회합니다. */
    @Transactional(readOnly = true)
    public AdminSecurityDtos.PageResponse searchSecurityEvents(Integer page, Integer size) {
        return search(false, page, size);
    }

    /** 민감정보 접근 로그를 조회합니다. */
    @Transactional(readOnly = true)
    public AdminSecurityDtos.PageResponse searchDataAccessLogs(Integer page, Integer size) {
        return search(true, page, size);
    }

    private AdminSecurityDtos.PageResponse search(boolean dataAccess, Integer page, Integer size) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size < 1 ? 20 : Math.min(size, 100);
        long total = repository.count(dataAccess);
        return new AdminSecurityDtos.PageResponse(repository.search(dataAccess, p, s), p, s, total, total == 0 ? 0 : (int) Math.ceil((double) total / s));
    }
}
