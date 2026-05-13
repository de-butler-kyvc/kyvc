package com.kyvc.backendadmin.domain.issuer.controller;

import com.kyvc.backendadmin.domain.issuer.application.IssuerConfigQueryService;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerConfigDetailResponse;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerConfigSummaryResponse;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Backend Admin Issuer 발급 설정 조회 API를 담당합니다. */
@Tag(name = "Backend Admin Issuer Config", description = "백엔드 관리자 Issuer 발급 설정 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/issuer-configs")
public class AdminIssuerConfigController {

    private final IssuerConfigQueryService issuerConfigQueryService;

    /** Issuer 발급 설정 목록을 조회합니다. */
    @Operation(summary = "Issuer 발급 설정 목록 조회", description = "Issuer 발급 설정 목록을 페이징, 상태, Issuer 유형, Credential 유형 조건으로 조회한다. 민감 기술 정보는 응답하지 않는다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Issuer 발급 설정 목록 조회 성공")})
    @GetMapping
    public CommonResponse<IssuerConfigSummaryResponse> search(
            @Parameter(description = "페이지 번호", example = "0") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(required = false) Integer size,
            @Parameter(description = "Issuer DID 또는 이름 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "설정 상태", example = "ACTIVE") @RequestParam(required = false) String status,
            @Parameter(description = "Issuer 유형", example = "PLATFORM") @RequestParam(required = false) String issuerType,
            @Parameter(description = "Credential 유형", example = "KYC_CREDENTIAL") @RequestParam(required = false) String credentialType
    ) {
        return CommonResponseFactory.success(issuerConfigQueryService.search(
                IssuerConfigSummaryResponse.SearchRequest.of(page, size, keyword, status, issuerType, credentialType)
        ));
    }

    /** Issuer 발급 설정 상세 정보를 조회합니다. */
    @Operation(summary = "Issuer 발급 설정 상세 조회", description = "Issuer 발급 설정 ID를 기준으로 상세 정보를 조회한다. signing key, private key, Core 내부 키 정보는 응답하지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issuer 발급 설정 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "Issuer 발급 설정을 찾을 수 없음")
    })
    @GetMapping("/{issuerConfigId}")
    public CommonResponse<IssuerConfigDetailResponse> getDetail(
            @Parameter(description = "Issuer 발급 설정 ID", required = true)
            @PathVariable Long issuerConfigId
    ) {
        return CommonResponseFactory.success(issuerConfigQueryService.getDetail(issuerConfigId));
    }
}
