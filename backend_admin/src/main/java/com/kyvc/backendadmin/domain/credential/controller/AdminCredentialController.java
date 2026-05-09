package com.kyvc.backendadmin.domain.credential.controller;

import com.kyvc.backendadmin.domain.credential.application.AdminCredentialIssueService;
import com.kyvc.backendadmin.domain.credential.application.AdminCredentialQueryService;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialDetailResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialRequestResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialStatusHistoryResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialSummaryResponse;
import com.kyvc.backendadmin.domain.credential.dto.CredentialIssueRequest;
import com.kyvc.backendadmin.domain.credential.dto.CredentialIssueResponse;
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
 * Backend Admin VC 발급 관리 API를 담당합니다.
 */
@Tag(name = "Backend Admin Credential", description = "백엔드 관리자 VC 발급 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend")
public class AdminCredentialController {

    private final AdminCredentialIssueService adminCredentialIssueService;
    private final AdminCredentialQueryService adminCredentialQueryService;

    /**
     * 승인된 KYC 신청 건에 대해 VC 발급을 요청합니다.
     *
     * @param kycId KYC 신청 ID
     * @param request VC 발급 요청 정보
     * @return VC 발급 요청 결과
     */
    @Operation(
            summary = "VC 발급 요청",
            description = "승인된 KYC 신청 건에 대해 VC 발급 요청을 생성합니다. 실제 VC 발급은 Core에서 처리하며, Backend Admin은 credentials와 core_requests row를 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "VC 발급 요청 생성 성공"),
            @ApiResponse(responseCode = "400", description = "VC 발급 요청이 불가능한 KYC 상태 또는 MFA 오류"),
            @ApiResponse(responseCode = "404", description = "KYC 신청을 찾을 수 없음")
    })
    @PostMapping("/kyc/applications/{kycId}/credentials/issue")
    public CommonResponse<CredentialIssueResponse> issue(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId,
            @RequestBody(
                    description = "VC 발급 요청 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CredentialIssueRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody CredentialIssueRequest request
    ) {
        return CommonResponseFactory.success(adminCredentialIssueService.issue(kycId, request));
    }

    /**
     * VC 발급 상태 목록을 조회합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param keyword 검색어
     * @param credentialStatus Credential 상태
     * @param coreRequestStatus Core 요청 상태
     * @param corporateName 법인명
     * @param businessRegistrationNumber 사업자등록번호
     * @param fromDate 조회 시작일
     * @param toDate 조회 종료일
     * @return VC 발급 상태 목록
     */
    @Operation(
            summary = "VC 발급 상태 목록 조회",
            description = "관리자가 VC 발급 상태 목록을 페이지, 상태, 법인명, 사업자등록번호, 기간 조건으로 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "VC 발급 상태 목록 조회 성공")
    @GetMapping("/credentials")
    public CommonResponse<AdminCredentialSummaryResponse> search(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "법인명 또는 사업자등록번호 검색어", example = "kyvc")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Credential 상태", example = "ISSUING")
            @RequestParam(required = false) String credentialStatus,
            @Parameter(description = "Core 요청 상태", example = "QUEUED")
            @RequestParam(required = false) String coreRequestStatus,
            @Parameter(description = "법인명", example = "케이와이브이씨")
            @RequestParam(required = false) String corporateName,
            @Parameter(description = "사업자등록번호", example = "123-45-67890")
            @RequestParam(required = false) String businessRegistrationNumber,
            @Parameter(description = "조회 시작일", example = "2026-05-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "조회 종료일", example = "2026-05-31")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate toDate
    ) {
        return CommonResponseFactory.success(adminCredentialQueryService.search(
                AdminCredentialSummaryResponse.SearchRequest.of(
                        page,
                        size,
                        keyword,
                        credentialStatus,
                        coreRequestStatus,
                        corporateName,
                        businessRegistrationNumber,
                        fromDate,
                        toDate
                )
        ));
    }

    /**
     * VC 발급 상세 정보를 조회합니다.
     *
     * @param credentialId Credential ID
     * @return VC 발급 상세 정보
     */
    @Operation(
            summary = "VC 발급 상세 조회",
            description = "특정 VC 발급 건의 KYC 정보, 법인 정보, VC 발급 상태, Core 요청 상태, XRPL 트랜잭션 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "VC 발급 상세 조회 성공"),
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
            description = "Credential ID 기준으로 발급, 폐기, 상태 확인 등의 요청 이력을 최신순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential 요청 이력 조회 성공"),
            @ApiResponse(responseCode = "404", description = "Credential을 찾을 수 없음")
    })
    @GetMapping("/credentials/{credentialId}/requests")
    public CommonResponse<List<AdminCredentialRequestResponse>> getCredentialRequests(
            @Parameter(description = "Credential ID", required = true)
            @PathVariable Long credentialId
    ) {
        return CommonResponseFactory.success(adminCredentialQueryService.getRequests(credentialId));
    }

    /**
     * Credential 상태 변경 이력을 조회합니다.
     *
     * @param credentialId Credential ID
     * @return Credential 상태 변경 이력 목록
     */
    @Operation(
            summary = "Credential 상태 이력 조회",
            description = "Credential ID 기준으로 상태 변경 이력을 최신순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credential 상태 이력 조회 성공"),
            @ApiResponse(responseCode = "404", description = "Credential을 찾을 수 없음")
    })
    @GetMapping("/credentials/{credentialId}/status-histories")
    public CommonResponse<List<AdminCredentialStatusHistoryResponse>> getCredentialStatusHistories(
            @Parameter(description = "Credential ID", required = true)
            @PathVariable Long credentialId
    ) {
        return CommonResponseFactory.success(adminCredentialQueryService.getStatusHistories(credentialId));
    }
}
