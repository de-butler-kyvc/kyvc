package com.kyvc.backend.domain.credential.controller;

import com.kyvc.backend.domain.credential.application.CredentialGuideService;
import com.kyvc.backend.domain.credential.dto.CredentialIssueGuideResponse;
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
 * VC 발급 안내 API Controller
 */
@RestController
@RequestMapping("/api/corporate/credentials")
@RequiredArgsConstructor
@Tag(name = "KYC 신청 / 서류", description = "KYC 신청, 필수서류, 문서 업로드, 제출, 완료 및 Credential Offer API")
public class CredentialGuideController {

    private final CredentialGuideService credentialGuideService;

    /**
     * VC 발급 안내 조회
     *
     * @param userDetails 인증 사용자 정보
     * @return VC 발급 안내 응답
     */
    @Operation(
            summary = "VC 발급 안내 조회",
            description = "로그인 사용자의 최신 KYC 상태와 Credential 상태를 기준으로 VC 발급 안내를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "VC 발급 안내 반환",
            content = @Content(schema = @Schema(implementation = CredentialIssueGuideResponse.class))
    )
    @GetMapping("/issue-guide")
    public ResponseEntity<CommonResponse<CredentialIssueGuideResponse>> getIssueGuide(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                credentialGuideService.getIssueGuide(getAuthenticatedUserId(userDetails))
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
