package com.kyvc.backendadmin.domain.credential.controller;

import com.kyvc.backendadmin.domain.credential.application.AdminCredentialIssueService;
import com.kyvc.backendadmin.domain.credential.application.AdminCredentialLifecycleService;
import com.kyvc.backendadmin.domain.credential.application.AdminCredentialQueryService;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialDetailResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialIssueRequest;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialIssueResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialListResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialRequestHistoryResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialSearchRequest;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialStatusHistoryResponse;
import com.kyvc.backendadmin.domain.credential.dto.CredentialActionResponse;
import com.kyvc.backendadmin.domain.credential.dto.CredentialReissueRequest;
import com.kyvc.backendadmin.domain.credential.dto.CredentialRevokeRequest;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Backend Admin Credential 및 VC 발급 상태 관리 API를 제공합니다.
 */
@Tag(name = "Backend Admin Credential", description = "관리자 Credential 및 VC 발급 상태 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend")
public class AdminCredentialController {

    private final AdminCredentialIssueService adminCredentialIssueService;
    private final AdminCredentialQueryService adminCredentialQueryService;
    private final AdminCredentialLifecycleService adminCredentialLifecycleService;

    /**
     * 승인된 KYC 신청 건에 대해 VC 발급을 요청합니다.
     *
     * @param kycId KYC 신청 ID
     * @param request VC 발급 요청 정보
     * @return VC 발급 요청 결과
     */
    @Operation(
            summary = "VC 발급 요청",
            description = "승인된 KYC 신청 건에 대해 Credential을 ISSUING 상태로 생성하고 Core VC_ISSUE 요청과 요청/상태 이력을 기록한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "VC 발급 요청 생성 성공"),
            @ApiResponse(responseCode = "401", description = "MFA 토큰이 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "KYC 신청을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "VC 발급이 불가능한 KYC 또는 Credential 상태")
    })
    @PostMapping("/kyc/applications/{kycId}/credentials/issue")
    public CommonResponse<AdminCredentialIssueResponse> issue(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId,
            @RequestBody(
                    description = "VC 발급 요청 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AdminCredentialIssueRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody AdminCredentialIssueRequest request
    ) {
        return CommonResponseFactory.success(adminCredentialIssueService.issue(kycId, request));
    }

    /**
     * Credential 목록을 검색합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status Credential 상태 코드
     * @param corporateName 법인명
     * @param businessRegistrationNo 사업자등록번호
     * @param issuerDid Issuer DID
     * @param fromDate 조회 시작일
     * @param toDate 조회 종료일
     * @return Credential 목록
     */
    @Operation(
            summary = "Credential 목록 조회",
            description = "Credential 목록을 페이지, 상태, 법인명, 사업자등록번호, Issuer DID, 생성일 조건으로 조회한다. 민감정보는 응답하지 않는다."
    )
    @ApiResponse(responseCode = "200", description = "Credential 목록 조회 성공")
    @GetMapping("/credentials")
    public CommonResponse<AdminCredentialListResponse> search(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Credential 상태 코드", example = "ISSUING")
            @RequestParam(required = false) String status,
            @Parameter(description = "법인명", example = "모의 재심사 법인")
            @RequestParam(required = false) String corporateName,
            @Parameter(description = "사업자등록번호", example = "999-88-77777")
            @RequestParam(required = false) String businessRegistrationNo,
            @Parameter(description = "Issuer DID", example = "did:kyvc:backend-admin")
            @RequestParam(required = false) String issuerDid,
            @Parameter(description = "조회 시작일", example = "2026-05-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "조회 종료일", example = "2026-05-31")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate toDate
    ) {
        return CommonResponseFactory.success(adminCredentialQueryService.search(
                AdminCredentialSearchRequest.of(
                        page,
                        size,
                        status,
                        corporateName,
                        businessRegistrationNo,
                        issuerDid,
                        fromDate,
                        toDate
                )
        ));
    }

    /**
     * Credential 상세 정보를 조회합니다.
     *
     * @param credentialId Credential ID
     * @return Credential 상세 정보
     */
    @Operation(
            summary = "Credential 상세 조회",
            description = "Credential 기본정보, 법인 정보, KYC 신청 정보, XRPL Tx 정보, Wallet 저장 여부, 최신 Core 요청 상태, 최근 상태 변경 이력을 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "Credential을 찾을 수 없음")
    })
    @GetMapping("/credentials/{credentialId}")
    public CommonResponse<AdminCredentialDetailResponse> getDetail(
            @Parameter(description = "Credential ID", required = true)
            @PathVariable Long credentialId
    ) {
        return CommonResponseFactory.success(adminCredentialQueryService.getDetail(credentialId));
    }

    /**
     * Credential 요청 이력을 조회합니다.
     *
     * @param credentialId Credential ID
     * @return Credential 요청 이력 목록
     */
    @Operation(
            summary = "Credential 요청 이력 조회",
            description = "Credential 발급, 재발급, 폐기 등 요청 이력을 최신순으로 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential 요청 이력 조회 성공"),
            @ApiResponse(responseCode = "404", description = "Credential을 찾을 수 없음")
    })
    @GetMapping("/credentials/{credentialId}/requests")
    public CommonResponse<List<AdminCredentialRequestHistoryResponse>> getRequestHistories(
            @Parameter(description = "Credential ID", required = true)
            @PathVariable Long credentialId
    ) {
        return CommonResponseFactory.success(adminCredentialQueryService.getRequestHistories(credentialId));
    }

    /**
     * Credential 상태 변경 이력을 조회합니다.
     *
     * @param credentialId Credential ID
     * @return Credential 상태 변경 이력 목록
     */
    @Operation(
            summary = "Credential 상태 변경 이력 조회",
            description = "Credential 상태가 ISSUING, VALID, REVOKED 등으로 변경된 이력을 최신순으로 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential 상태 변경 이력 조회 성공"),
            @ApiResponse(responseCode = "404", description = "Credential을 찾을 수 없음")
    })
    @GetMapping("/credentials/{credentialId}/status-histories")
    public CommonResponse<List<AdminCredentialStatusHistoryResponse>> getStatusHistories(
            @Parameter(description = "Credential ID", required = true)
            @PathVariable Long credentialId
    ) {
        return CommonResponseFactory.success(adminCredentialQueryService.getStatusHistories(credentialId));
    }

    /**
     * VC 재발급을 Backend API로 요청합니다.
     *
     * @param credentialId Credential ID
     * @param request VC 재발급 요청 정보
     * @return VC 재발급 요청 결과
     */
    @Operation(
            summary = "VC 재발급 요청",
            description = "관리자가 선택한 VC에 대해 Backend API로 재발급을 요청한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "VC 재발급 요청 성공"),
            @ApiResponse(responseCode = "401", description = "MFA 토큰이 유효하지 않은 경우"),
            @ApiResponse(responseCode = "404", description = "Credential을 찾을 수 없는 경우"),
            @ApiResponse(responseCode = "409", description = "재발급할 수 없는 Credential 상태인 경우")
    })
    @PostMapping("/credentials/{credentialId}/reissue")
    public CommonResponse<CredentialActionResponse> reissue(
            @Parameter(description = "Credential ID", required = true, example = "1")
            @PathVariable Long credentialId,
            @RequestBody(
                    description = "VC 재발급 요청 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CredentialReissueRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody CredentialReissueRequest request
    ) {
        return CommonResponseFactory.success(adminCredentialLifecycleService.reissue(credentialId, request));
    }

    /**
     * VC 폐기를 Backend API로 요청합니다.
     *
     * @param credentialId Credential ID
     * @param request VC 폐기 요청 정보
     * @return VC 폐기 요청 결과
     */
    @Operation(
            summary = "VC 폐기 요청",
            description = "관리자가 선택한 VC에 대해 Backend API로 폐기를 요청한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "VC 폐기 요청 성공"),
            @ApiResponse(responseCode = "401", description = "MFA 토큰이 유효하지 않은 경우"),
            @ApiResponse(responseCode = "404", description = "Credential을 찾을 수 없는 경우"),
            @ApiResponse(responseCode = "409", description = "폐기할 수 없는 Credential 상태인 경우")
    })
    @PostMapping("/credentials/{credentialId}/revoke")
    public CommonResponse<CredentialActionResponse> revoke(
            @Parameter(description = "Credential ID", required = true, example = "1")
            @PathVariable Long credentialId,
            @RequestBody(
                    description = "VC 폐기 요청 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CredentialRevokeRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody CredentialRevokeRequest request
    ) {
        return CommonResponseFactory.success(adminCredentialLifecycleService.revoke(credentialId, request));
    }
}
