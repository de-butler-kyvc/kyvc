package com.kyvc.backend.domain.vp.controller;

import com.kyvc.backend.domain.vp.application.UserVpPresentationService;
import com.kyvc.backend.domain.vp.dto.UserVpPresentationListResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 VP 제출 이력 API Controller
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User VP Presentations", description = "사용자 VP 제출 이력 API")
public class UserVpPresentationController {

    private final UserVpPresentationService userVpPresentationService;

    /**
     * 사용자 VP 제출 이력 목록을 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status 검증 상태 필터
     * @param verifierName Verifier명 필터
     * @return 사용자 VP 제출 이력 목록 응답
     */
    @Operation(
            summary = "사용자 VP 제출 이력 조회",
            description = "로그인 사용자의 법인 Credential로 제출된 VP 이력만 조회합니다. VP JWT, VC 원문, Core raw payload는 반환하지 않습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "사용자 VP 제출 이력 목록 반환",
            content = @Content(schema = @Schema(implementation = UserVpPresentationListResponse.class))
    )
    @GetMapping("/vp-presentations")
    public CommonResponse<UserVpPresentationListResponse> getPresentations(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @RequestParam(required = false) Integer page, // 페이지 번호
            @RequestParam(required = false) Integer size, // 페이지 크기
            @RequestParam(required = false) String status, // 검증 상태 필터
            @RequestParam(required = false) String verifierName // Verifier명 필터
    ) {
        return CommonResponseFactory.success(
                userVpPresentationService.getPresentations(userDetails, page, size, status, verifierName)
        );
    }
}
