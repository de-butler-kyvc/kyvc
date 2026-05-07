package com.kyvc.backendadmin.domain.kyc.controller;

import com.kyvc.backendadmin.domain.kyc.application.AdminKycApplicationService;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationCorporateResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationDetailResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationListResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationSearchRequest;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * KYC 신청 목록 및 신청 법인정보 조회 API를 담당합니다.
 *
 * <p>백엔드 관리자가 KYC 신청 목록을 검색하고, KYC 신청 ID 기준으로 신청 법인정보를 조회하는
 * 엔드포인트를 제공합니다.</p>
 */
@Tag(name = "Backend Admin KYC", description = "백엔드 관리자 KYC 심사 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/kyc/applications")
public class AdminKycApplicationController {

    private final AdminKycApplicationService adminKycApplicationService;

    /**
     * KYC 신청 목록을 검색합니다.
     *
     * <p>page는 0부터 시작하는 페이지 번호, size는 페이지 크기입니다.
     * status는 KYC 신청 상태, keyword는 법인명/사용자 이메일/사업자등록번호 검색어입니다.
     * submittedFrom과 submittedTo는 제출일 검색 범위이며, aiReviewStatus는 AI 심사 상태,
     * supplementYn은 최신 보완요청 존재 여부를 의미합니다.</p>
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status KYC 신청 상태
     * @param keyword 법인명, 사용자 이메일, 사업자등록번호 검색어
     * @param submittedFrom 제출일 시작일
     * @param submittedTo 제출일 종료일
     * @param aiReviewStatus AI 심사 상태
     * @param supplementYn 보완요청 여부
     * @return KYC 신청 목록 응답
     */
    @Operation(summary = "KYC 신청 목록 조회", description = "kyc_applications, corporates, users, kyc_supplements를 조인하여 KYC 신청 목록을 조회합니다.")
    @GetMapping
    public CommonResponse<AdminKycApplicationListResponse> searchApplications(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "KYC 신청 상태", example = "SUBMITTED")
            @RequestParam(required = false) String status,
            @Parameter(description = "법인명, 사용자 이메일, 사업자등록번호 검색어", example = "kyvc")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "제출일 시작일", example = "2026-05-01", schema = @Schema(type = "string", format = "date"))
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate submittedFrom,
            @Parameter(description = "제출일 종료일", example = "2026-05-31", schema = @Schema(type = "string", format = "date"))
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate submittedTo,
            @Parameter(description = "AI 심사 상태", example = "SUCCESS")
            @RequestParam(required = false) String aiReviewStatus,
            @Parameter(description = "보완요청 여부(Y/N)", example = "Y")
            @RequestParam(required = false) String supplementYn
    ) {
        AdminKycApplicationSearchRequest request = AdminKycApplicationSearchRequest.of(
                page,
                size,
                status,
                keyword,
                submittedFrom,
                submittedTo,
                aiReviewStatus,
                supplementYn
        );
        return CommonResponseFactory.success(adminKycApplicationService.searchApplications(request));
    }

    /**
     * KYC 신청 상세 정보를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return KYC 신청 상세 정보
     */
    @Operation(
            summary = "KYC 신청 상세 조회",
            description = "특정 KYC 신청 건의 기본 정보, 법인 정보, 제출 문서 요약, AI 심사 상태, VC 발급 상태를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KYC 신청 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "KYC 신청을 찾을 수 없음")
    })
    @GetMapping("/{kycId}")
    public CommonResponse<AdminKycApplicationDetailResponse> getApplicationDetail(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId
    ) {
        return CommonResponseFactory.success(adminKycApplicationService.getApplicationDetail(kycId));
    }

    /**
     * KYC 신청 법인정보를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return KYC 신청 법인정보 응답
     */
    @Operation(summary = "KYC 신청 법인정보 조회", description = "KYC 신청 존재 여부를 확인한 뒤 신청 법인정보를 조회합니다.")
    @ApiResponse(responseCode = "404", description = "KYC 신청이 없는 경우")
    @GetMapping("/{kycId}/corporate")
    public CommonResponse<AdminKycApplicationCorporateResponse> getApplicationCorporate(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId
    ) {
        return CommonResponseFactory.success(adminKycApplicationService.getApplicationCorporate(kycId));
    }
}
