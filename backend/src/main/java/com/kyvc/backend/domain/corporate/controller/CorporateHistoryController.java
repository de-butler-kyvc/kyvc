package com.kyvc.backend.domain.corporate.controller;

import com.kyvc.backend.domain.corporate.application.CorporateHistoryService;
import com.kyvc.backend.domain.corporate.dto.CorporateChangeHistoryResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 법인 변경 이력 API Controller
 */
@RestController
@RequestMapping("/api/user/corporates/{corporateId}/change-histories")
@RequiredArgsConstructor
@Tag(name = "법인 사용자", description = "법인 사용자 대시보드, 법인 기본정보, 대표자, 대리인 관리 API")
public class CorporateHistoryController {

    private final CorporateHistoryService corporateHistoryService;

    /**
     * 법인 변경 이력 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param corporateId 법인 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 법인 변경 이력 페이지 응답
     */
    @Operation(
            summary = "법인 변경 이력 조회",
            description = "로그인 사용자가 소유한 법인의 감사로그 기반 변경 이력을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "법인 변경 이력 페이지 반환",
            content = @Content(schema = @Schema(implementation = CorporateChangeHistoryResponse.class))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<CorporateChangeHistoryResponse>> getChangeHistories(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "법인 ID", example = "1")
            @PathVariable Long corporateId, // 법인 ID
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page, // 페이지 번호
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size // 페이지 크기
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                corporateHistoryService.getChangeHistories(
                        getAuthenticatedUserId(userDetails),
                        corporateId,
                        page,
                        size
                )
        ));
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
