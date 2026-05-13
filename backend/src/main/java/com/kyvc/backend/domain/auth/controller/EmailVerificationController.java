package com.kyvc.backend.domain.auth.controller;

import com.kyvc.backend.domain.auth.application.EmailVerificationService;
import com.kyvc.backend.domain.auth.dto.EmailVerificationRequest;
import com.kyvc.backend.domain.auth.dto.EmailVerificationRequestResponse;
import com.kyvc.backend.domain.auth.dto.EmailVerificationVerifyRequest;
import com.kyvc.backend.domain.auth.dto.EmailVerificationVerifyResponse;
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
 * 회원가입 이메일 인증 API Controller
 */
@RestController
@RequestMapping("/api/auth/email-verifications")
@RequiredArgsConstructor
@Tag(name = "인증 / 계정", description = "사용자 인증, 계정, 개발 토큰, 모바일 로그인 및 비밀번호 재설정 API")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    /**
     * 회원가입 이메일 인증번호를 발송한다.
     *
     * @param request 회원가입 이메일 인증번호 발송 요청
     * @return 회원가입 이메일 인증번호 발송 응답
     */
    @Operation(
            summary = "회원가입 이메일 인증번호 발송",
            description = """
                    비로그인 사용자가 회원가입 화면에서 입력한 이메일로 6자리 인증번호를 발송합니다.
                    JWT/Cookie 인증이 필요하지 않습니다.
                    이 API는 로그인 사용자 MFA API가 아닙니다.
                    기존 POST /api/auth/mfa/challenge는 JWT/Cookie 기반 로그인 사용자 MFA용입니다.
                    인증번호 원문은 저장하지 않고 hash만 저장합니다.
                    검증 성공 여부는 프론트에서 회원가입 버튼 활성화 등 화면 제어에 사용합니다.
                    회원가입 API에서 emailVerificationToken을 강제 검증하지 않습니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "회원가입 이메일 인증번호 발송 응답 반환",
            content = @Content(schema = @Schema(implementation = EmailVerificationRequestResponse.class))
    )
    @PostMapping("/request")
    public ResponseEntity<CommonResponse<EmailVerificationRequestResponse>> request(
            @Valid @RequestBody EmailVerificationRequest request // 회원가입 이메일 인증번호 발송 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(emailVerificationService.request(request)));
    }

    /**
     * 회원가입 이메일 인증번호를 검증한다.
     *
     * @param request 회원가입 이메일 인증번호 검증 요청
     * @return 회원가입 이메일 인증번호 검증 응답
     */
    @Operation(
            summary = "회원가입 이메일 인증번호 검증",
            description = """
                    비로그인 사용자가 회원가입 화면에서 입력한 verificationId, email, 6자리 인증번호를 검증합니다.
                    JWT/Cookie 인증이 필요하지 않습니다.
                    이 API는 로그인 사용자 MFA 검증 API가 아닙니다.
                    기존 POST /api/auth/mfa/verify는 JWT/Cookie 기반 로그인 사용자 MFA 검증용이며, 성공 시 mfaToken을 반환합니다.
                    이 API는 성공 시 emailVerificationToken을 발급하지 않고 verified=true와 email만 반환합니다.
                    auth_tokens 테이블을 사용하지 않습니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "회원가입 이메일 인증번호 검증 응답 반환",
            content = @Content(schema = @Schema(implementation = EmailVerificationVerifyResponse.class))
    )
    @PostMapping("/verify")
    public ResponseEntity<CommonResponse<EmailVerificationVerifyResponse>> verify(
            @Valid @RequestBody EmailVerificationVerifyRequest request // 회원가입 이메일 인증번호 검증 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(emailVerificationService.verify(request)));
    }
}
