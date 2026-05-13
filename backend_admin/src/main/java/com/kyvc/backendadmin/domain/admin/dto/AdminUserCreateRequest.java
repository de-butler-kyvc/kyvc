package com.kyvc.backendadmin.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 관리자 계정 생성 요청 DTO
/**
 * 관리자 계정 생성 요청 DTO입니다.
 *
 * <p>신규 관리자 계정의 이메일, 이름, 초기 비밀번호, 상태, 권한 ID 목록을 전달합니다.</p>
 */
@Schema(description = "관리자 계정 생성 요청")
public record AdminUserCreateRequest(
        @Schema(description = "관리자 이메일", example = "admin@kyvc.com")
        @Email @NotBlank String email, // 관리자 이메일
        @Schema(description = "관리자 이름", example = "Backend Admin")
        @NotBlank String name, // 관리자 이름
        @Schema(description = "초기 비밀번호", example = "Password123!")
        @NotBlank String password, // 초기 비밀번호
        @Schema(description = "관리자 상태", example = "ACTIVE")
        @NotBlank String status, // 관리자 상태
        @Schema(description = "할당할 권한 ID 목록", example = "[1,2]")
        @NotEmpty List<Long> roleIds // 할당할 권한 ID 목록
) {
}
