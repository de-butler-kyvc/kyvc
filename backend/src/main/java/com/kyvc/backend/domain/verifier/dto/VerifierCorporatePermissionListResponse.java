package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Verifier 기업 권한 확인 목록 응답
 *
 * @param items 권한 확인 결과 목록
 * @param page 페이징 정보
 */
@Schema(description = "Verifier 기업 권한 확인 목록 응답")
public record VerifierCorporatePermissionListResponse(
        @Schema(description = "권한 확인 결과 목록")
        List<Item> items, // 권한 확인 결과 목록
        @Schema(description = "페이징 정보")
        PageInfo page // 페이징 정보
) {

    /**
     * Verifier 기업 권한 확인 항목
     *
     * @param corporateId 법인 ID
     * @param corporateName 법인명
     * @param permissionCode 권한 코드
     * @param permissionStatus 권한 상태
     * @param verificationStatus 검증 상태
     * @param lastVerifiedAt 마지막 검증 일시
     */
    public record Item(
            @Schema(description = "법인 ID", example = "1")
            Long corporateId, // 법인 ID
            @Schema(description = "법인명", example = "주식회사 KYVC")
            String corporateName, // 법인명
            @Schema(description = "권한 코드", example = "ACCOUNT_OPENING")
            String permissionCode, // 권한 코드
            @Schema(description = "권한 상태", example = "VALID")
            String permissionStatus, // 권한 상태
            @Schema(description = "검증 상태", example = "VALID")
            String verificationStatus, // 검증 상태
            @Schema(description = "마지막 검증 일시", example = "2026-05-11T10:00:00")
            LocalDateTime lastVerifiedAt // 마지막 검증 일시
    ) {
    }

    /**
     * 페이징 정보
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param totalElements 전체 건수
     * @param totalPages 전체 페이지 수
     */
    public record PageInfo(
            @Schema(description = "페이지 번호", example = "0")
            int page, // 페이지 번호
            @Schema(description = "페이지 크기", example = "20")
            int size, // 페이지 크기
            @Schema(description = "전체 건수", example = "1")
            long totalElements, // 전체 건수
            @Schema(description = "전체 페이지 수", example = "1")
            int totalPages // 전체 페이지 수
    ) {
    }
}
