package com.kyvc.backendadmin.domain.issuer.controller;

import com.kyvc.backendadmin.domain.issuer.application.IssuerPolicyQueryService;
import com.kyvc.backendadmin.domain.issuer.application.IssuerPolicyService;
import com.kyvc.backendadmin.domain.issuer.dto.*;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Backend Admin Issuer 신뢰 정책 관리 API를 담당합니다. */
@Tag(name = "Backend Admin Issuer Policy", description = "백엔드 관리자 Issuer 신뢰 정책 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/issuer-policies")
public class AdminIssuerPolicyController {

    private final IssuerPolicyQueryService issuerPolicyQueryService;
    private final IssuerPolicyService issuerPolicyService;

    /** Issuer 정책 목록을 조회합니다. */
    @Operation(summary = "Issuer 정책 목록 조회", description = "Issuer 신뢰 정책 목록을 페이징, 정책 유형, 상태, Issuer DID, Issuer 이름, Credential 유형 조건으로 조회한다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Issuer 정책 목록 조회 성공")})
    @GetMapping
    public CommonResponse<IssuerPolicySummaryResponse> search(
            @Parameter(description = "페이지 번호", example = "0") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(required = false) Integer size,
            @Parameter(description = "검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "정책 유형", example = "WHITELIST") @RequestParam(required = false) String policyType,
            @Parameter(description = "정책 상태", example = "ACTIVE") @RequestParam(required = false) String status,
            @Parameter(description = "Issuer DID") @RequestParam(required = false) String issuerDid,
            @Parameter(description = "Issuer 이름") @RequestParam(required = false) String issuerName,
            @Parameter(description = "Credential 유형", example = "KYC_CREDENTIAL") @RequestParam(required = false) String credentialType
    ) {
        return CommonResponseFactory.success(issuerPolicyQueryService.search(
                IssuerPolicySummaryResponse.SearchRequest.of(page, size, keyword, policyType, status, issuerDid, issuerName, credentialType)));
    }

    /** Issuer 정책 상세 정보를 조회합니다. */
    @Operation(summary = "Issuer 정책 상세 조회", description = "Issuer 정책 ID를 기준으로 정책 상세 정보를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issuer 정책 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "Issuer 정책을 찾을 수 없음")
    })
    @GetMapping("/{policyId}")
    public CommonResponse<IssuerPolicyResponse> getDetail(
            @Parameter(description = "Issuer 정책 ID", required = true) @PathVariable Long policyId
    ) {
        return CommonResponseFactory.success(issuerPolicyQueryService.getDetail(policyId));
    }

    /** Issuer 화이트리스트 정책을 등록합니다. */
    @Operation(summary = "Issuer 화이트리스트 등록", description = "신뢰 가능한 Issuer를 화이트리스트 정책으로 등록한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issuer 화이트리스트 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 또는 MFA 토큰이 유효하지 않음"),
            @ApiResponse(responseCode = "409", description = "중복 또는 충돌 정책")
    })
    @PostMapping("/whitelist")
    public CommonResponse<IssuerPolicyResponse> createWhitelist(
            @RequestBody(description = "Issuer 화이트리스트 등록 요청", required = true,
                    content = @Content(schema = @Schema(implementation = IssuerPolicyWhitelistCreateRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody IssuerPolicyWhitelistCreateRequest request
    ) {
        return CommonResponseFactory.success(issuerPolicyService.createWhitelist(request));
    }

    /** Issuer 블랙리스트 정책을 등록합니다. */
    @Operation(summary = "Issuer 블랙리스트 등록", description = "검증을 거부할 Issuer를 블랙리스트 정책으로 등록한다. 블랙리스트는 화이트리스트보다 우선한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issuer 블랙리스트 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 또는 MFA 토큰이 유효하지 않음"),
            @ApiResponse(responseCode = "409", description = "중복 정책")
    })
    @PostMapping("/blacklist")
    public CommonResponse<IssuerPolicyResponse> createBlacklist(
            @RequestBody(description = "Issuer 블랙리스트 등록 요청", required = true,
                    content = @Content(schema = @Schema(implementation = IssuerPolicyBlacklistCreateRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody IssuerPolicyBlacklistCreateRequest request
    ) {
        return CommonResponseFactory.success(issuerPolicyService.createBlacklist(request));
    }

    /** Issuer 정책을 수정합니다. */
    @Operation(summary = "Issuer 정책 수정", description = "Issuer 정책의 이름, Credential 유형, 상태, 사유를 수정한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issuer 정책 수정 성공"),
            @ApiResponse(responseCode = "404", description = "Issuer 정책을 찾을 수 없음")
    })
    @PatchMapping("/{policyId}")
    public CommonResponse<IssuerPolicyResponse> update(
            @Parameter(description = "Issuer 정책 ID", required = true) @PathVariable Long policyId,
            @RequestBody(description = "Issuer 정책 수정 요청", required = true,
                    content = @Content(schema = @Schema(implementation = IssuerPolicyUpdateRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody IssuerPolicyUpdateRequest request
    ) {
        return CommonResponseFactory.success(issuerPolicyService.update(policyId, request));
    }

    /** Issuer 정책을 비활성화합니다. */
    @Operation(summary = "Issuer 정책 비활성화", description = "Issuer 정책을 물리 삭제하지 않고 비활성화한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issuer 정책 비활성화 성공"),
            @ApiResponse(responseCode = "404", description = "Issuer 정책을 찾을 수 없음")
    })
    @DeleteMapping("/{policyId}")
    public CommonResponse<IssuerPolicyResponse> disable(
            @Parameter(description = "Issuer 정책 ID", required = true) @PathVariable Long policyId
    ) {
        return CommonResponseFactory.success(issuerPolicyService.disable(policyId));
    }
}
