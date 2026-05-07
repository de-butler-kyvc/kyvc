package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * KYC 심사 이력 조회 응답 DTO입니다.
 */
@Schema(description = "KYC 심사 이력 조회 응답")
public record KycReviewHistoryResponse(

        /** 심사 이력 ID */
        @Schema(description = "심사 이력 ID", example = "1000")
        Long historyId,

        /** 심사 처리 유형 */
        @Schema(description = "심사 처리 유형", example = "APPROVE")
        String actionType,

        /** 행위자 유형 */
        @Schema(description = "행위자 유형", example = "ADMIN")
        String actorType,

        /** 행위자 ID */
        @Schema(description = "행위자 ID", example = "1")
        Long actorId,

        /** 행위자 표시명 */
        @Schema(description = "행위자 표시명", example = "관리자")
        String actorName,

        /** 처리 의견 */
        @Schema(description = "처리 의견", example = "심사 승인")
        String comment,

        /** 이력 생성 시각 */
        @Schema(description = "이력 생성 시각")
        LocalDateTime createdAt
) {
}
