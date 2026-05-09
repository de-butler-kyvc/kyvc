package com.kyvc.backend.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 법인 기본정보 최초등록 요청
 *
 * @param corporateName 법인명
 * @param businessRegistrationNo 사업자등록번호
 * @param corporateRegistrationNo 법인등록번호
 * @param corporateTypeCode 법인 유형 코드
 * @param establishedDate 설립일
 * @param corporatePhone 법인 대표전화
 * @param address 법인 주소
 * @param website 웹사이트 주소
 */
@Schema(description = "법인 기본정보 최초등록 요청")
public record CorporateCreateRequest(
        @Schema(description = "법인명", example = "주식회사 케이와이브이씨")
        @NotBlank(message = "법인명은 필수입니다.")
        String corporateName, // 법인명
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        @NotBlank(message = "사업자등록번호는 필수입니다.")
        String businessRegistrationNo, // 사업자등록번호
        @Schema(description = "법인등록번호", example = "110111-1234567")
        String corporateRegistrationNo, // 법인등록번호
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateTypeCode, // 법인 유형 코드
        @Schema(description = "설립일", example = "2020-01-01")
        LocalDate establishedDate, // 설립일
        @Schema(description = "법인 대표전화", example = "02-1234-5678")
        @Size(max = 50, message = "법인 대표전화는 50자 이하여야 합니다.")
        String corporatePhone, // 법인 대표전화
        @Schema(description = "법인 주소", example = "서울특별시 강남구 테헤란로 1")
        String address, // 법인 주소
        @Schema(description = "웹사이트 주소", example = "https://kyvc.local")
        String website // 웹사이트 주소
) {
}
