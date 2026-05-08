package com.kyvc.backend.domain.auth.controller;

import com.kyvc.backend.domain.auth.application.SessionService;
import com.kyvc.backend.domain.auth.dto.SessionResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 세션 조회 API Controller
 */
@RestController
@RequestMapping("/api/common/session")
@RequiredArgsConstructor
@Tag(name = "인증 / 계정", description = "사용자 인증, 계정, 개발 토큰, 모바일 로그인 및 비밀번호 재설정 API")
public class SessionController {

    private final SessionService sessionService;

    /**
     * 현재 세션 조회
     *
     * @param userDetails 인증 사용자 정보
     * @return 세션 응답
     */
    @Operation(
            summary = "현재 세션 조회",
            description = "현재 로그인 사용자의 인증 여부, 사용자 정보, 권한, 법인 등록 상태를 조회합니다. 입력값은 없으며 JWT 인증 정보로 사용자를 식별합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "인증 여부, 사용자 ID, 이메일, 사용자 유형, 권한 목록, 법인 ID, 법인 등록 여부 반환",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<SessionResponse>> getSession(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(sessionService.getSession(userDetails)));
    }
}
