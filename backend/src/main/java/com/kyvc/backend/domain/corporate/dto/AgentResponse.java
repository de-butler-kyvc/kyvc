package com.kyvc.backend.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 대리인 정보 응답
 *
 * @param agentId 대리인 ID
 * @param corporateId 법인 ID
 * @param name 대리인명
 * @param relationshipOrPosition 대리인 관계/직책
 * @param phoneNumber 대리인 연락처
 * @param email 대리인 이메일
 * @param delegationDocumentId 위임장 문서 ID
 */
@Schema(description = "대리인 정보 응답")
public record AgentResponse(
        @Schema(description = "대리인 ID", example = "1")
        Long agentId, // 대리인 ID
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "대리인명", example = "김대리")
        String name, // 대리인명
        @Schema(description = "대리인 관계/직책", example = "팀장")
        String relationshipOrPosition, // 대리인 관계/직책
        @Schema(description = "대리인 연락처", example = "010-9876-5432")
        String phoneNumber, // 대리인 연락처
        @Schema(description = "대리인 이메일", example = "agent@kyvc.local")
        String email, // 대리인 이메일
        @Schema(description = "위임장 문서 ID", example = "1")
        Long delegationDocumentId // 위임장 문서 ID
) {
}
