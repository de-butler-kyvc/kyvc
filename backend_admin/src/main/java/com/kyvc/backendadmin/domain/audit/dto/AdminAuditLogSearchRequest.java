package com.kyvc.backendadmin.domain.audit.dto;

import java.time.LocalDateTime;

/**
 * 감사로그 목록 검색 조건을 전달하는 DTO입니다.
 */
public record AdminAuditLogSearchRequest(
        int page,
        int size,
        String actorType,
        Long actorId,
        String actionType,
        String targetType,
        Long targetId,
        LocalDateTime from,
        LocalDateTime to
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public static AdminAuditLogSearchRequest of(
            Integer page,
            Integer size,
            String actorType,
            Long actorId,
            String actionType,
            String targetType,
            Long targetId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        int resolvedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int resolvedSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new AdminAuditLogSearchRequest(
                resolvedPage,
                resolvedSize,
                actorType,
                actorId,
                actionType,
                targetType,
                targetId,
                from,
                to
        );
    }
}
