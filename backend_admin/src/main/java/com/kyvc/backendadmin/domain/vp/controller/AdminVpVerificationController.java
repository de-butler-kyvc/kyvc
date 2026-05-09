package com.kyvc.backendadmin.domain.vp.controller;

import com.kyvc.backendadmin.domain.vp.application.AdminVpVerificationQueryService;
import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationDetailResponse;
import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationListResponse;
import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationSearchRequest;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * Backend Admin VP 검증 관리자 조회 API를 제공합니다.
 */
@Tag(name = "Backend Admin VP Verification", description = "관리자 VP 검증 요청/제출/결과 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/vp-verifications")
public class AdminVpVerificationController {

    private final AdminVpVerificationQueryService adminVpVerificationQueryService;

    /**
     * VP 검증 목록을 검색합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status VP 검증 상태 코드
     * @param corporateId 법인 ID
     * @param credentialId Credential ID
     * @param verifierId Verifier ID
     * @param requestTypeCode 요청 유형 코드
     * @param replaySuspectedYn Replay 의심 여부
     * @param testYn 테스트 요청 여부
     * @param fromDate 요청일 조회 시작일
     * @param toDate 요청일 조회 종료일
     * @param keyword 키워드
     * @return VP 검증 목록
     */
    @Operation(
            summary = "VP 검증 목록 조회",
            description = "VP 검증 요청, 제출, 검증 결과를 상태, 법인, Credential, Verifier, replay 의심 여부, 테스트 여부, 요청일, 키워드 조건으로 조회한다."
    )
    @ApiResponse(responseCode = "200", description = "VP 검증 목록 조회 성공")
    @GetMapping
    public CommonResponse<AdminVpVerificationListResponse> search(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "VP 검증 상태 코드", example = "VALID")
            @RequestParam(required = false) String status,
            @Parameter(description = "법인 ID", example = "900003")
            @RequestParam(required = false) Long corporateId,
            @Parameter(description = "Credential ID", example = "1")
            @RequestParam(required = false) Long credentialId,
            @Parameter(description = "Verifier ID", example = "1")
            @RequestParam(required = false) Long verifierId,
            @Parameter(description = "요청 유형 코드", example = "PRESENTATION")
            @RequestParam(required = false) String requestTypeCode,
            @Parameter(description = "Replay 의심 여부", example = "N")
            @RequestParam(required = false) String replaySuspectedYn,
            @Parameter(description = "테스트 요청 여부", example = "N")
            @RequestParam(required = false) String testYn,
            @Parameter(description = "요청일 조회 시작일", example = "2026-05-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "요청일 조회 종료일", example = "2026-05-31")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate toDate,
            @Parameter(description = "법인명, 요청자명, 목적 검색 키워드", example = "대출")
            @RequestParam(required = false) String keyword
    ) {
        return CommonResponseFactory.success(adminVpVerificationQueryService.search(
                AdminVpVerificationSearchRequest.of(
                        page,
                        size,
                        status,
                        corporateId,
                        credentialId,
                        verifierId,
                        requestTypeCode,
                        replaySuspectedYn,
                        testYn,
                        fromDate,
                        toDate,
                        keyword
                )
        ));
    }

    /**
     * VP 검증 상세 정보를 조회합니다.
     *
     * @param verificationId VP 검증 ID
     * @return VP 검증 상세 정보
     */
    @Operation(
            summary = "VP 검증 상세 조회",
            description = "VP 검증 요청, 제출, 검증 결과, replay 의심 여부, Core 요청 상태, Credential 정보, Verifier 정보를 상세 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "VP 검증 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "VP 검증 정보를 찾을 수 없음")
    })
    @GetMapping("/{verificationId}")
    public CommonResponse<AdminVpVerificationDetailResponse> getDetail(
            @Parameter(description = "VP 검증 ID", required = true, example = "1")
            @PathVariable Long verificationId
    ) {
        return CommonResponseFactory.success(adminVpVerificationQueryService.getDetail(verificationId));
    }
}
