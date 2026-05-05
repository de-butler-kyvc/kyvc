package com.kyvc.backendadmin.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 법인 유형별 필수서류 정책 목록 검색 조건 DTO입니다.
 *
 * <p>관리자 KYC 정책 목록 조회에서 페이지, 법인 유형, 문서 유형,
 * 필수 여부, 사용 여부 조건을 표현합니다.</p>
 */
@Schema(description = "필수서류 정책 목록 검색 조건")
public record AdminDocumentRequirementSearchRequest(
        @Schema(description = "페이지 번호, 0부터 시작", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "법인 유형 공통코드", example = "CORPORATION")
        String corporateType,
        @Schema(description = "문서 유형 공통코드", example = "BUSINESS_REGISTRATION")
        String documentType,
        @Schema(description = "필수 여부", example = "Y", allowableValues = {"Y", "N"})
        String requiredYn,
        @Schema(description = "사용 여부", example = "Y", allowableValues = {"Y", "N"})
        String enabledYn
) {

    /**
     * 요청 파라미터의 페이지 기본값과 범위를 보정하여 검색 조건을 생성합니다.
     *
     * @param page 요청 페이지 번호
     * @param size 요청 페이지 크기
     * @param corporateType 법인 유형 공통코드
     * @param documentType 문서 유형 공통코드
     * @param requiredYn 필수 여부
     * @param enabledYn 사용 여부
     * @return 보정된 필수서류 정책 검색 조건
     */
    public static AdminDocumentRequirementSearchRequest of(
            Integer page,
            Integer size,
            String corporateType,
            String documentType,
            String requiredYn,
            String enabledYn
    ) {
        int resolvedPage = page == null || page < 0 ? 0 : page;
        int resolvedSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return new AdminDocumentRequirementSearchRequest(
                resolvedPage,
                resolvedSize,
                corporateType,
                documentType,
                requiredYn,
                enabledYn
        );
    }
}
