package com.kyvc.backend.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 법인 기본정보 수정 요청
 *
 * @param corporateName 법인명
 * @param businessRegistrationNo 사업자등록번호
 * @param corporateRegistrationNo 법인등록번호
 * @param representativeName 대표자명
 * @param representativePhone 대표자 연락처
 * @param representativeEmail 대표자 이메일
 * @param address 법인 주소
 * @param businessType 업종
 */
@Schema(description = "법인 기본정보 수정 요청")
public record CorporateBasicInfoRequest(
        @Schema(description = "법인명", example = "주식회사 케이와이브이씨")
        @NotBlank(message = "법인명은 필수입니다.")
        String corporateName, // 법인명
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        @NotBlank(message = "사업자등록번호는 필수입니다.")
        String businessRegistrationNo, // 사업자등록번호
        @Schema(description = "법인등록번호", example = "110111-1234567")
        String corporateRegistrationNo, // 법인등록번호
        @Schema(description = "대표자명", example = "홍길동")
        @NotBlank(message = "대표자명은 필수입니다.")
        String representativeName, // 대표자명
        @Schema(description = "대표자 연락처", example = "010-1234-5678")
        String representativePhone, // 대표자 연락처
        @Schema(description = "대표자 이메일", example = "representative@kyvc.local")
        @Email(message = "대표자 이메일은 올바른 이메일 형식이어야 합니다.")
        String representativeEmail, // 대표자 이메일
        @Schema(description = "법인 주소", example = "서울특별시 강남구 테헤란로 1")
        String address, // 법인 주소
        @Schema(description = "업종", example = "소프트웨어 개발")
        String businessType // 업종
) {
}
