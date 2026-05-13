package com.kyvc.backend.domain.kyc.controller;

import com.kyvc.backend.domain.kyc.application.KycApplicationHistoryService;
import com.kyvc.backend.domain.kyc.dto.KycApplicationHistoryResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * KYC 신청 이력 API Controller
 */
@RestController
@RequestMapping("/api/corporate/kyc/applications/history")
@RequiredArgsConstructor
@Tag(name = "KYC 신청 / 서류", description = "KYC 신청, 필수서류, 문서 업로드, 제출, 완료 및 Credential Offer API")
public class KycApplicationHistoryController {

    private final KycApplicationHistoryService kycApplicationHistoryService;

    /**
     * KYC 신청 이력 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param status KYC 상태 코드
     * @param keyword 검색어
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return KYC 신청 이력 페이지 응답
     */
    @Operation(
            summary = "KYC 신청 이력 조회",
            description = "로그인 사용자가 소유한 법인의 KYC 신청 이력을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "KYC 신청 이력 페이지 반환",
            content = @Content(schema = @Schema(implementation = KycApplicationHistoryResponse.class))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<KycApplicationHistoryResponse>> getHistory(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 상태 코드", example = "APPROVED")
            @RequestParam(required = false) String status, // KYC 상태 코드
            @Parameter(description = "검색어", example = "케이와이브이씨")
            @RequestParam(required = false) String keyword, // 검색어
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page, // 페이지 번호
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size // 페이지 크기
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycApplicationHistoryService.getHistory(
                        getAuthenticatedUserId(userDetails),
                        status,
                        keyword,
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
