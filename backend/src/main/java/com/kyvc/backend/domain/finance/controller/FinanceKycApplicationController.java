package com.kyvc.backend.domain.finance.controller;

import com.kyvc.backend.domain.finance.application.FinanceKycApplicationService;
import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationCreateRequest;
import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationCreateResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationDetailResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationListResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycCorporateUpdateRequest;
import com.kyvc.backend.domain.finance.dto.FinanceKycCorporateUpdateResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 금융사 방문 KYC API Controller
 */
@RestController
@RequestMapping("/api/finance/kyc/applications")
@RequiredArgsConstructor
@Tag(name = "Finance Visit KYC", description = "금융사 방문 KYC 기본 흐름 API")
public class FinanceKycApplicationController {

    private final FinanceKycApplicationService financeKycApplicationService;

    /**
     * 금융사 방문 KYC 생성
     *
     * @param userDetails 인증 사용자 정보
     * @param request 금융사 방문 KYC 생성 요청
     * @return 금융사 방문 KYC 생성 응답
     */
    @Operation(
            summary = "금융사 방문 KYC 생성",
            description = "금융사 직원이 연결된 법인 고객번호 기준으로 방문 KYC 초안을 생성합니다."
    )
    @ApiResponse(
            responseCode = "201",
            description = "금융사 방문 KYC 생성 결과",
            content = @Content(schema = @Schema(implementation = FinanceKycApplicationCreateResponse.class))
    )
    @PostMapping
    public ResponseEntity<CommonResponse<FinanceKycApplicationCreateResponse>> createApplication(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "금융사 방문 KYC 생성 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FinanceKycApplicationCreateRequest.class))
            )
            @Valid @RequestBody FinanceKycApplicationCreateRequest request // 금융사 방문 KYC 생성 요청
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponseFactory.success(
                        financeKycApplicationService.createApplication(userDetails, request)
                ));
    }

    /**
     * 금융사 방문 KYC 목록 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status KYC 상태 코드
     * @param keyword 검색어
     * @return 금융사 방문 KYC 목록 응답
     */
    @Operation(
            summary = "금융사 방문 KYC 목록 조회",
            description = "금융사 직원 본인이 생성한 방문 KYC 신청 목록을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "금융사 방문 KYC 목록 반환",
            content = @Content(schema = @Schema(implementation = FinanceKycApplicationListResponse.class))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<FinanceKycApplicationListResponse>> getApplications(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @RequestParam(required = false) Integer page, // 페이지 번호
            @RequestParam(required = false) Integer size, // 페이지 크기
            @RequestParam(required = false) String status, // KYC 상태 코드
            @RequestParam(required = false) String keyword // 검색어
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                financeKycApplicationService.getApplications(userDetails, status, keyword, page, size)
        ));
    }

    /**
     * 금융사 방문 KYC 상세 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return 금융사 방문 KYC 상세 응답
     */
    @Operation(
            summary = "금융사 방문 KYC 상세 조회",
            description = "금융사 직원 본인이 생성한 방문 KYC 신청 상세와 제출서류 요약을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "금융사 방문 KYC 상세 반환",
            content = @Content(schema = @Schema(implementation = FinanceKycApplicationDetailResponse.class))
    )
    @GetMapping("/{kycId}")
    public ResponseEntity<CommonResponse<FinanceKycApplicationDetailResponse>> getApplication(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                financeKycApplicationService.getApplication(userDetails, kycId)
        ));
    }

    /**
     * 금융사 방문 KYC 법인정보 수정
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @param request 금융사 방문 KYC 법인정보 수정 요청
     * @return 금융사 방문 KYC 법인정보 수정 응답
     */
    @Operation(
            summary = "금융사 방문 KYC 법인정보 수정",
            description = "제출 전 금융사 방문 KYC의 연결 법인정보를 수정합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "금융사 방문 KYC 법인정보 수정 결과",
            content = @Content(schema = @Schema(implementation = FinanceKycCorporateUpdateResponse.class))
    )
    @PutMapping("/{kycId}/corporate")
    public ResponseEntity<CommonResponse<FinanceKycCorporateUpdateResponse>> updateCorporate(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long kycId, // KYC 신청 ID
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "금융사 방문 KYC 법인정보 수정 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FinanceKycCorporateUpdateRequest.class))
            )
            @Valid @RequestBody FinanceKycCorporateUpdateRequest request // 금융사 방문 KYC 법인정보 수정 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                financeKycApplicationService.updateCorporate(userDetails, kycId, request)
        ));
    }
}
