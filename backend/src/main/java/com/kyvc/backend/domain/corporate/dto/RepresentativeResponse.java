package com.kyvc.backend.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 대표자 정보 응답
 *
 * @param representativeId 대표자 ID
 * @param corporateId 법인 ID
 * @param name 대표자명
 * @param phoneNumber 대표자 연락처
 * @param email 대표자 이메일
 */
@Schema(description = "대표자 정보 응답")
public record RepresentativeResponse(
        @Schema(description = "대표자 ID", example = "1")
        Long representativeId, // 대표자 ID
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "대표자명", example = "홍길동")
        String name, // 대표자명
        @Schema(description = "대표자 연락처", example = "010-1234-5678")
        String phoneNumber, // 대표자 연락처
        @Schema(description = "대표자 이메일", example = "representative@kyvc.local")
        String email // 대표자 이메일
) {
}
