package com.kyvc.backendadmin.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 법인 사용자 계정 등록 요청 DTO입니다.
 */
@Schema(description = "법인 사용자 계정 등록 요청")
public record AdminCorporateUserCreateRequest(
        /** 로그인 이메일 */
        @Schema(description = "로그인 이메일", example = "corp-user@kyvc.com")
        String email,
        /** 초기 비밀번호 */
        @Schema(description = "초기 비밀번호", example = "Password123!")
        String password,
        /** 법인명 */
        @Schema(description = "법인명", example = "케이와이브이씨")
        String corporateName,
        /** 사업자등록번호 */
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessRegistrationNo,
        /** 법인등록번호 */
        @Schema(description = "법인등록번호", example = "110111-1234567")
        String corporateRegistrationNo,
        /** 대표자명 */
        @Schema(description = "대표자명", example = "홍길동")
        String representativeName,
        /** 대표자 전화번호 */
        @Schema(description = "대표자 전화번호", example = "010-1234-5678")
        String representativePhone,
        /** 대표자 이메일 */
        @Schema(description = "대표자 이메일", example = "ceo@kyvc.com")
        String representativeEmail,
        /** 주소 */
        @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 1")
        String address,
        /** 업종 */
        @Schema(description = "업종", example = "핀테크")
        String businessType,
        /** 계정 상태 */
        @Schema(description = "계정 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "LOCKED", "WITHDRAWN"})
        String status
) {
}
