package com.kyvc.backend.domain.commoncode.dto;

/**
 * 공통코드 조회 항목
 *
 * @param codeGroup 공통코드 그룹 코드
 * @param code 공통코드 값
 * @param codeName 공통코드 표시명
 * @param description 공통코드 설명
 * @param sortOrder 정렬 순서
 */
public record CommonCodeItem(
        String codeGroup, // 공통코드 그룹 코드
        String code, // 공통코드 값
        String codeName, // 공통코드 표시명
        String description, // 공통코드 설명
        Integer sortOrder // 정렬 순서
) {

    /**
     * @return 공통코드 그룹 코드
     */
    public String codeGroup() {
        return codeGroup;
    }

    /**
     * @return 공통코드 값
     */
    public String code() {
        return code;
    }

    /**
     * @return 공통코드 표시명
     */
    public String codeName() {
        return codeName;
    }

    /**
     * @return 공통코드 설명
     */
    public String description() {
        return description;
    }

    /**
     * @return 정렬 순서
     */
    public Integer sortOrder() {
        return sortOrder;
    }
}
