package com.kyvc.backend.domain.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 모바일 로그인 요청
 *
 * @param email 로그인 이메일
 * @param password 로그인 비밀번호
 * @param deviceId 모바일 기기 ID
 * @param deviceName 모바일 기기명
 * @param os 모바일 운영체제 정보
 * @param appVersion 앱 버전
 * @param publicKey 기기 공개키
 */
@Schema(description = "모바일 로그인 요청")
public record MobileLoginRequest(
        @Schema(description = "로그인 이메일", example = "user@kyvc.local")
        @NotBlank(message = "이메일은 필수입니다.")
        String email, // 로그인 이메일
        @Schema(description = "로그인 비밀번호", example = "password123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password, // 로그인 비밀번호
        @Schema(description = "모바일 기기 ID", example = "device-001")
        @NotBlank(message = "기기 ID는 필수입니다.")
        String deviceId, // 모바일 기기 ID
        @Schema(description = "모바일 기기명", example = "iPhone 15")
        String deviceName, // 모바일 기기명
        @Schema(description = "모바일 운영체제 정보", example = "iOS")
        @NotBlank(message = "운영체제 정보는 필수입니다.")
        String os, // 모바일 운영체제 정보
        @Schema(description = "앱 버전", example = "1.0.0")
        String appVersion, // 앱 버전
        @Schema(description = "기기 공개키", example = "public-key-ref")
        String publicKey // 기기 공개키
) {
}
