package com.kyvc.backendadmin.global.commoncode.dto;

/**
 * 공통코드 그룹 목록 조회 조건을 전달하는 DTO입니다.
 */
public record CommonCodeGroupSearchRequest(
        int page,
        int size,
        String codeGroup,
        String keyword,
        String enabledYn
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public static CommonCodeGroupSearchRequest of(
            Integer page,
            Integer size,
            String codeGroup,
            String keyword,
            String enabledYn
    ) {
        int resolvedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int resolvedSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new CommonCodeGroupSearchRequest(resolvedPage, resolvedSize, codeGroup, keyword, enabledYn);
    }
}
