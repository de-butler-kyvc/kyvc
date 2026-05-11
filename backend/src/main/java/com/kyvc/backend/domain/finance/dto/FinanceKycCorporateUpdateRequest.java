package com.kyvc.backend.domain.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 방문 KYC 법인정보 수정 요청
 *
 * @param corporateName 법인명
 * @param businessRegistrationNo 사업자등록번호
 * @param corporateRegistrationNo 법인등록번호
 * @param representativeName 대표자명
 * @param corporateTypeCode 법인 유형 코드
 * @param address 법인 주소
 */
@Schema(description = "금융사 방문 KYC 법인정보 수정 요청")
public record FinanceKycCorporateUpdateRequest(
        @Schema(description = "법인명", example = "주식회사 KYVC")
        String corporateName, // 법인명
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessRegistrationNo, // 사업자등록번호
        @Schema(description = "법인등록번호", example = "110111-1234567")
        String corporateRegistrationNo, // 법인등록번호
        @Schema(description = "대표자명", example = "홍길동")
        String representativeName, // 대표자명
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateTypeCode, // 법인 유형 코드
        @Schema(description = "법인 주소", example = "서울시")
        String address // 법인 주소
) {
}
