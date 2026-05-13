package com.kyvc.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 정보 응답
 *
 * @param userId 사용자 ID
 * @param email 로그인 이메일
 * @param userName 사용자명
 * @param phone 사용자 연락처
 * @param userTypeCode 사용자 유형 코드
 * @param userStatusCode 사용자 상태 코드
 * @param notificationEnabledYn 알림 수신 여부
 * @param mfaEnabledYn MFA 사용 여부
 * @param mfaTypeCode MFA 유형 코드
 * @param lastPasswordChangedAt 마지막 비밀번호 변경 일시
 * @param createdAt 생성 일시
 * @param updatedAt 수정 일시
 * @param roles 보유 역할 목록
 */
@Schema(description = "내 정보 응답")
public record UserMeResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId, // 사용자 ID
        @Schema(description = "로그인 이메일", example = "user@kyvc.local")
        String email, // 로그인 이메일
        @Schema(description = "사용자명", example = "홍길동")
        String userName, // 사용자명
        @Schema(description = "사용자 연락처", example = "010-1234-5678")
        String phone, // 사용자 연락처
        @Schema(description = "사용자 유형 코드", example = "CORPORATE_USER")
        String userTypeCode, // 사용자 유형 코드
        @Schema(description = "사용자 상태 코드", example = "ACTIVE")
        String userStatusCode, // 사용자 상태 코드
        @Schema(description = "알림 수신 여부", example = "Y")
        String notificationEnabledYn, // 알림 수신 여부
        @Schema(description = "MFA 사용 여부", example = "N")
        String mfaEnabledYn, // MFA 사용 여부
        @Schema(description = "MFA 유형 코드", example = "EMAIL", nullable = true)
        String mfaTypeCode, // MFA 유형 코드
        @Schema(description = "마지막 비밀번호 변경 일시", example = "2026-05-11T10:30:00")
        LocalDateTime lastPasswordChangedAt, // 마지막 비밀번호 변경 일시
        @Schema(description = "생성 일시", example = "2026-05-11T10:00:00")
        LocalDateTime createdAt, // 생성 일시
        @Schema(description = "수정 일시", example = "2026-05-11T10:30:00")
        LocalDateTime updatedAt, // 수정 일시
        @Schema(description = "보유 역할 목록")
        List<RoleItem> roles // 보유 역할 목록
) {

    public UserMeResponse {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    /**
     * 보유 역할 항목
     *
     * @param roleCode 역할 코드
     * @param roleName 역할명
     * @param roleTypeCode 역할 유형 코드
     */
    @Schema(description = "보유 역할 항목")
    public record RoleItem(
            @Schema(description = "역할 코드", example = "CORPORATE_USER")
            String roleCode, // 역할 코드
            @Schema(description = "역할명", example = "법인 사용자")
            String roleName, // 역할명
            @Schema(description = "역할 유형 코드", example = "USER")
            String roleTypeCode // 역할 유형 코드
    ) {
    }
}
