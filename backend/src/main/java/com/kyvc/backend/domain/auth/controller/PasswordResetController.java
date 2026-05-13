package com.kyvc.backend.domain.auth.controller;

import com.kyvc.backend.domain.auth.application.PasswordResetService;
import com.kyvc.backend.domain.auth.dto.PasswordResetConfirmRequest;
import com.kyvc.backend.domain.auth.dto.PasswordResetConfirmResponse;
import com.kyvc.backend.domain.auth.dto.PasswordResetRequest;
import com.kyvc.backend.domain.auth.dto.PasswordResetRequestResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비밀번호 재설정 API Controller
 */
@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
@Tag(name = "인증 / 계정", description = "사용자 인증, 계정, 개발 토큰, 모바일 로그인 및 비밀번호 재설정 API")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * 비밀번호 재설정 요청 생성
     *
     * @param request 비밀번호 재설정 요청 생성 요청
     * @return 비밀번호 재설정 요청 생성 응답
     */
    @Operation(
            summary = "비밀번호 재설정 메일 요청",
            description = """
                    비로그인 사용자가 입력한 이메일 기준으로 비밀번호 재설정 메일 또는 토큰을 요청합니다.
                    JWT/Cookie 인증이 필요하지 않습니다.
                    이 API는 회원가입 이메일 인증번호 발송 API가 아닙니다.
                    회원가입 화면 이메일 인증번호 발송은 POST /api/auth/email-verifications/request를 사용합니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "비밀번호 재설정 요청 생성 응답 반환",
            content = @Content(schema = @Schema(implementation = PasswordResetRequestResponse.class))
    )
    @PostMapping("/request")
    public ResponseEntity<CommonResponse<PasswordResetRequestResponse>> request(
            @Valid @RequestBody PasswordResetRequest request // 비밀번호 재설정 요청 생성 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(passwordResetService.request(request)));
    }

    /**
     * 비밀번호 재설정 확정
     *
     * @param request 비밀번호 재설정 확정 요청
     * @return 비밀번호 재설정 확정 응답
     */
    @Operation(
            summary = "비밀번호 재설정 확정",
            description = """
                    비로그인 사용자가 비밀번호 재설정 토큰과 새 비밀번호를 제출해 비밀번호를 변경합니다.
                    JWT/Cookie 인증이 필요하지 않습니다.
                    이 API는 MFA 인증번호 검증이나 회원가입 이메일 인증번호 검증 API가 아닙니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "비밀번호 재설정 확정 응답 반환",
            content = @Content(schema = @Schema(implementation = PasswordResetConfirmResponse.class))
    )
    @PostMapping("/confirm")
    public ResponseEntity<CommonResponse<PasswordResetConfirmResponse>> confirm(
            @Valid @RequestBody PasswordResetConfirmRequest request // 비밀번호 재설정 확정 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(passwordResetService.confirm(request)));
    }
}
