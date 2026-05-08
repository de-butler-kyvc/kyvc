package com.kyvc.backend.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 법인정보 응답
 *
 * @param corporateId 법인 ID
 * @param userId 사용자 ID
 * @param corporateName 법인명
 * @param businessRegistrationNo 사업자등록번호
 * @param corporateRegistrationNo 법인등록번호
 * @param corporateTypeCode 법인 유형 코드
 * @param establishedDate 설립일
 * @param representativeName 대표자명
 * @param representativePhone 대표자 연락처
 * @param representativeEmail 대표자 이메일
 * @param address 법인 주소
 * @param website 웹사이트 주소
 * @param businessType 업종
 * @param corporateStatusCode 법인 상태 코드
 * @param createdAt 생성일시
 * @param updatedAt 수정일시
 */
@Schema(description = "법인정보 응답")
public record CorporateResponse(
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "사용자 ID", example = "1")
        Long userId, // 사용자 ID
        @Schema(description = "법인명", example = "주식회사 케이와이브이씨")
        String corporateName, // 법인명
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessRegistrationNo, // 사업자등록번호
        @Schema(description = "법인등록번호", example = "110111-1234567")
        String corporateRegistrationNo, // 법인등록번호
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateTypeCode, // 법인 유형 코드
        @Schema(description = "설립일", example = "2020-01-01")
        LocalDate establishedDate, // 설립일
        @Schema(description = "대표자명", example = "홍길동")
        String representativeName, // 대표자명
        @Schema(description = "대표자 연락처", example = "010-1234-5678")
        String representativePhone, // 대표자 연락처
        @Schema(description = "대표자 이메일", example = "representative@kyvc.local")
        String representativeEmail, // 대표자 이메일
        @Schema(description = "법인 주소", example = "서울특별시 강남구 테헤란로 1")
        String address, // 법인 주소
        @Schema(description = "웹사이트 주소", example = "https://kyvc.local")
        String website, // 웹사이트 주소
        @Schema(description = "업종", example = "소프트웨어 개발")
        String businessType, // 업종
        @Schema(description = "법인 상태 코드", example = "ACTIVE")
        String corporateStatusCode, // 법인 상태 코드
        @Schema(description = "생성일시", example = "2026-05-04T12:30:00")
        LocalDateTime createdAt, // 생성일시
        @Schema(description = "수정일시", example = "2026-05-04T12:30:00")
        LocalDateTime updatedAt // 수정일시
) {
}
