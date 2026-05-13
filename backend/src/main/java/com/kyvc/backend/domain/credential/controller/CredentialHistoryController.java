package com.kyvc.backend.domain.credential.controller;

import com.kyvc.backend.domain.credential.application.CredentialHistoryService;
import com.kyvc.backend.domain.credential.dto.CredentialStatusHistoryResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Credential 상태 이력 API Controller
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "Credential History", description = "사용자 VC 상태 이력 API")
public class CredentialHistoryController {

    private final CredentialHistoryService credentialHistoryService;

    /**
     * Credential 상태 변경 이력을 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param credentialId Credential ID
     * @return Credential 상태 변경 이력 응답
     */
    @Operation(
            summary = "사용자 VC 상태 변경 이력 조회",
            description = "로그인 사용자가 소유한 Credential 상태 변경 이력을 조회합니다. VC 원문과 Core raw payload는 반환하지 않습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Credential 상태 변경 이력 반환",
            content = @Content(schema = @Schema(implementation = CredentialStatusHistoryResponse.class))
    )
    @GetMapping("/credentials/{credentialId}/histories")
    public CommonResponse<CredentialStatusHistoryResponse> getCredentialHistories(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long credentialId // Credential ID
    ) {
        return CommonResponseFactory.success(
                credentialHistoryService.getCredentialHistories(userDetails, credentialId)
        );
    }
}
