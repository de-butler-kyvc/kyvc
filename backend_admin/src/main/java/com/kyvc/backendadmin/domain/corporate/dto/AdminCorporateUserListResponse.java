package com.kyvc.backendadmin.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 법인 사용자 목록 응답 DTO입니다.
 *
 * <p>검색된 법인 사용자 목록과 페이지 정보를 관리자 화면에 전달합니다.</p>
 */
@Schema(description = "법인 사용자 목록 응답")
public record AdminCorporateUserListResponse(
        @Schema(description = "법인 사용자 목록")
        List<Item> items,
        @Schema(description = "현재 페이지 번호", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "전체 건수", example = "120")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "6")
        int totalPages
) {

    /**
     * 법인 사용자 목록의 단일 항목 DTO입니다.
     */
    @Schema(description = "법인 사용자 목록 항목")
    public record Item(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,
            @Schema(description = "사용자 이메일", example = "corp@kyvc.local")
            String email,
            @Schema(description = "사용자 상태", example = "ACTIVE")
            String userStatus,
            @Schema(description = "법인 ID", example = "10")
            Long corporateId,
            @Schema(description = "법인명", example = "케이와이브이씨")
            String corporateName,
            @Schema(description = "법인 상태", example = "ACTIVE")
            String corporateStatus,
            @Schema(description = "최근 KYC ID", example = "100")
            Long latestKycId,
            @Schema(description = "최근 KYC 상태", example = "APPROVED")
            String latestKycStatus,
            @Schema(description = "사용자 생성 시각")
            LocalDateTime createdAt
    ) {
    }
}
