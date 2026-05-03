package com.kyvc.backend.domain.auth.controller;

import com.kyvc.backend.domain.auth.application.SessionService;
import com.kyvc.backend.domain.auth.dto.SessionResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
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
public class SessionController {

    private final SessionService sessionService;

    /**
     * 현재 세션 조회
     *
     * @param userDetails 인증 사용자 정보
     * @return 세션 응답
     */
    @GetMapping
    public ResponseEntity<CommonResponse<SessionResponse>> getSession(
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(sessionService.getSession(userDetails)));
    }
}
