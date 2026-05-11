package com.kyvc.backend.domain.finance.controller;

import com.kyvc.backend.domain.finance.application.FinanceContextService;
import com.kyvc.backend.domain.finance.dto.FinanceMeResponse;
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
 * 금융사 직원 컨텍스트 API Controller
 */
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@Tag(name = "Finance Context", description = "금융사 직원 컨텍스트 API")
public class FinanceMeController {

    private final FinanceContextService financeContextService;

    /**
     * 금융사 직원 컨텍스트 조회
     *
     * @param userDetails 인증 사용자 정보
     * @return 금융사 직원 컨텍스트 응답
     */
    @Operation(
            summary = "금융사 직원 컨텍스트 조회",
            description = "로그인한 금융사 직원의 기관, 지점, 권한 컨텍스트를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "금융사 직원 컨텍스트 반환",
            content = @Content(schema = @Schema(implementation = FinanceMeResponse.class))
    )
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<FinanceMeResponse>> getMe(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(financeContextService.getMe(userDetails)));
    }
}
