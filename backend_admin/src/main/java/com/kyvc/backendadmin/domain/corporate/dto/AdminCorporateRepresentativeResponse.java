package com.kyvc.backendadmin.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 법인 대표자 조회 응답 DTO입니다.
 */
@Schema(description = "법인 대표자 조회 응답")
public record AdminCorporateRepresentativeResponse(
        @Schema(description = "대표자 ID", example = "1")
        Long representativeId,
        @Schema(description = "법인 ID", example = "10")
        Long corporateId,
        @Schema(description = "대표자명", example = "홍길동")
        String representativeName,
        @Schema(description = "생년월일", example = "1980-01-01")
        LocalDate birthDate,
        @Schema(description = "국적 코드", example = "KR")
        String nationalityCode,
        @Schema(description = "전화번호", example = "010-1234-5678")
        String phone,
        @Schema(description = "이메일", example = "ceo@kyvc.local")
        String email,
        @Schema(description = "신분증 문서 ID", example = "100")
        Long identityDocumentId,
        @Schema(description = "신분증 문서명", example = "representative-id.pdf")
        String identityDocumentName,
        @Schema(description = "활성 여부", example = "Y")
        String activeYn,
        @Schema(description = "생성 일시")
        LocalDateTime createdAt,
        @Schema(description = "수정 일시")
        LocalDateTime updatedAt
) {
}
