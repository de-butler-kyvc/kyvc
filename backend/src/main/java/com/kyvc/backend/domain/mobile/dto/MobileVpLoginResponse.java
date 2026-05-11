package com.kyvc.backend.domain.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모바일 VP 로그인 응답
 *
 * @param loggedIn 로그인 성공 여부
 * @param userId 사용자 ID
 * @param corporateId 법인 ID
 * @param email 사용자 이메일
 * @param userType 사용자 유형
 * @param roles 권한 목록
 * @param credentialId Credential ID
 * @param vpVerificationId VP 검증 ID
 * @param accessTokenExpiresAt Access Token 만료 일시
 */
@Schema(description = "모바일 VP 로그인 응답")
public record MobileVpLoginResponse(
        @Schema(description = "로그인 성공 여부", example = "true")
        Boolean loggedIn, // 로그인 성공 여부
        @Schema(description = "사용자 ID", example = "1")
        Long userId, // 사용자 ID
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "사용자 이메일", example = "user@kyvc.local")
        String email, // 사용자 이메일
        @Schema(description = "사용자 유형", example = "CORPORATE_USER")
        String userType, // 사용자 유형
        @Schema(description = "권한 목록")
        List<String> roles, // 권한 목록
        @Schema(description = "Credential ID", example = "1")
        Long credentialId, // Credential ID
        @Schema(description = "VP 검증 ID", example = "10")
        Long vpVerificationId, // VP 검증 ID
        @Schema(description = "Access Token 만료 일시", example = "2026-05-11T13:00:00")
        LocalDateTime accessTokenExpiresAt // Access Token 만료 일시
) {
}
