package com.kyvc.backend.domain.user.controller;

import com.kyvc.backend.domain.user.application.UserDashboardService;
import com.kyvc.backend.domain.user.dto.UserDashboardResponse;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
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
 * 법인 사용자 대시보드 API Controller
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "법인 사용자 대시보드", description = "로그인 사용자의 법인 사용자 대시보드 API")
public class UserDashboardController {

    private final UserDashboardService userDashboardService;

    /**
     * 법인 사용자 대시보드 조회
     *
     * @param userDetails 인증 사용자 정보
     * @return 법인 사용자 대시보드 응답
     */
    @Operation(
            summary = "법인 사용자 대시보드 조회",
            description = "로그인 사용자의 법인정보 등록 여부와 대시보드 기본 상태를 조회합니다. 입력값은 없습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "사용자 ID, 법인 등록 여부, 법인 ID, 법인명, KYC 및 알림 기본 상태 반환",
            content = @Content(schema = @Schema(implementation = UserDashboardResponse.class))
    )
    @GetMapping("/dashboard")
    public ResponseEntity<CommonResponse<UserDashboardResponse>> getDashboard(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(userDashboardService.getDashboard(getAuthenticatedUserId(userDetails)))
        );
    }

    // 인증 사용자 ID 조회
    private Long getAuthenticatedUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
