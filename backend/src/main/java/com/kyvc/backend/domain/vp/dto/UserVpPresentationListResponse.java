package com.kyvc.backend.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 VP 제출 이력 목록 응답
 *
 * @param items VP 제출 이력 목록
 * @param page 페이지 정보
 */
@Schema(description = "사용자 VP 제출 이력 목록 응답")
public record UserVpPresentationListResponse(
        @Schema(description = "VP 제출 이력 목록")
        List<Item> items, // VP 제출 이력 목록
        @Schema(description = "페이지 정보")
        PageInfo page // 페이지 정보
) {

    public UserVpPresentationListResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * 사용자 VP 제출 이력 항목
     *
     * @param presentationId VP 제출 ID
     * @param requestId VP 요청 ID
     * @param verifierName Verifier명
     * @param purpose 제출 목적
     * @param verificationStatus 검증 상태 코드
     * @param presentedAt 제출 일시
     */
    @Schema(description = "사용자 VP 제출 이력 항목")
    public record Item(
            @Schema(description = "VP 제출 ID", example = "1")
            Long presentationId, // VP 제출 ID
            @Schema(description = "VP 요청 ID", example = "vp-req-001")
            String requestId, // VP 요청 ID
            @Schema(description = "Verifier명", example = "Verifier")
            String verifierName, // Verifier명
            @Schema(description = "제출 목적", example = "ACCOUNT_OPENING")
            String purpose, // 제출 목적
            @Schema(description = "검증 상태 코드", example = "VALID")
            String verificationStatus, // 검증 상태 코드
            @Schema(description = "제출 일시", example = "2026-05-11T10:00:00")
            LocalDateTime presentedAt // 제출 일시
    ) {
    }

    /**
     * 페이지 정보
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param totalElements 전체 건수
     * @param totalPages 전체 페이지 수
     */
    @Schema(description = "페이지 정보")
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
