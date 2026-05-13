package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 웹 VP 로그인 모바일 제출 요청
 *
 * @param qrToken QR 토큰 원문
 * @param credentialId Credential ID
 * @param vp Wallet 생성 VP 객체
 * @param deviceId 모바일 기기 ID
 * @param selectedDisclosurePaths 선택 disclosure 경로 목록
 */
@Schema(description = "웹 VP 로그인 모바일 제출 요청")
public record WebVpLoginSubmitRequest(
        @Schema(description = "QR 토큰 원문")
        String qrToken, // QR 토큰 원문
        @Schema(description = "Credential ID", example = "200")
        Long credentialId, // Credential ID
        @Schema(description = "Wallet 생성 VP 객체")
        Object vp, // Wallet 생성 VP 객체
        @Schema(description = "모바일 기기 ID", example = "android-device-id")
        String deviceId, // 모바일 기기 ID
        @Schema(description = "선택 disclosure 경로 목록")
        List<String> selectedDisclosurePaths // 선택 disclosure 경로 목록
) {
}
