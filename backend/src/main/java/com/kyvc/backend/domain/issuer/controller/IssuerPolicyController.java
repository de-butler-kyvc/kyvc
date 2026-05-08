package com.kyvc.backend.domain.issuer.controller;

import com.kyvc.backend.domain.issuer.application.IssuerPolicyService;
import com.kyvc.backend.domain.issuer.dto.EffectiveIssuerPolicyResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issuer 정책 조회 API Controller
 */
@RestController
@RequestMapping("/api/internal/issuer-policies")
@RequiredArgsConstructor
@Tag(name = "Core 연동 / Callback", description = "Core 내부 헬스체크, Issuer 정책, AI/VC/XRPL/VP callback 및 개발용 E2E API")
public class IssuerPolicyController {

    private final IssuerPolicyService issuerPolicyService;

    /**
     * 유효 Issuer 정책 조회
     *
     * @param credentialTypeCode Credential 유형 코드
     * @return 유효 Issuer 정책 응답
     */
    @Operation(
            summary = "유효 Issuer 정책 조회",
            description = "Credential 유형 코드 기준의 활성 Issuer 정책 목록 조회"
    )
    @ApiResponse(
            responseCode = "200",
            description = "유효 Issuer 정책 반환",
            content = @Content(schema = @Schema(implementation = EffectiveIssuerPolicyResponse.class))
    )
    @GetMapping("/effective")
    public ResponseEntity<CommonResponse<EffectiveIssuerPolicyResponse>> getEffectivePolicies(
            @Parameter(description = "Credential 유형 코드", example = "KYC_CREDENTIAL")
            @RequestParam(required = false) String credentialTypeCode // Credential 유형 코드
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                issuerPolicyService.getEffectivePolicies(credentialTypeCode)
        ));
    }
}
