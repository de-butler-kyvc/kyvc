package com.kyvc.backend.domain.verifier.controller;

import com.kyvc.backend.domain.verifier.application.VerifierVpService;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestCreateRequest;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestCreateResponse;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestDetailResponse;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestListResponse;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 금융사 Verifier VP API Controller
 */
@RestController
@RequestMapping("/api/finance/verifier")
@RequiredArgsConstructor
@Tag(name = "Finance Verifier VP", description = "금융사 VP 요청 생성과 조회 API")
public class FinanceVerifierController {

    private final VerifierVpService verifierVpService;

    /**
     * 금융사 VP 검증 요청을 생성
     *
     * @param userDetails 인증 사용자 정보
     * @param request 금융사 VP 요청 생성 요청
     * @return 금융사 VP 요청 생성 응답
     */
    @Operation(
            summary = "금융사 VP 검증 요청 생성",
            description = "금융사 VP 검증 요청과 QR payload를 생성합니다. 본 API는 Core를 호출하지 않으며, 실제 VP 검증은 모바일 VP 제출 API에서 동기 처리합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "금융사 VP 요청 생성 응답",
            content = @Content(schema = @Schema(implementation = FinanceVpRequestCreateResponse.class))
    )
    @PostMapping("/vp-requests")
    public CommonResponse<FinanceVpRequestCreateResponse> createVpRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Valid @RequestBody FinanceVpRequestCreateRequest request // 금융사 VP 요청 생성 요청
    ) {
        return CommonResponseFactory.success(verifierVpService.createFinanceVpRequest(userDetails, request));
    }

    /**
     * 금융사 VP 검증 요청 목록을 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param status VP 검증 상태 필터
     * @param from 조회 시작 일시
     * @param to 조회 종료 일시
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 금융사 VP 요청 목록 응답
     */
    @Operation(
            summary = "금융사 VP 검증 요청 목록 조회",
            description = "금융사 VP 검증 요청 목록을 조회합니다. Core를 직접 호출하지 않고 backend DB에 저장된 VP 요청 상태만 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "금융사 VP 요청 목록 응답",
            content = @Content(schema = @Schema(implementation = FinanceVpRequestListResponse.class))
    )
    @GetMapping("/vp-requests")
    public CommonResponse<FinanceVpRequestListResponse> getVpRequests(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @RequestParam(required = false) String status, // VP 검증 상태 필터
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from, // 조회 시작 일시
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, // 조회 종료 일시
            @RequestParam(required = false) Integer page, // 페이지 번호
            @RequestParam(required = false) Integer size // 페이지 크기
    ) {
        return CommonResponseFactory.success(
                verifierVpService.getFinanceVpRequests(userDetails, status, from, to, page, size)
        );
    }

    /**
     * 금융사 VP 검증 요청 상세와 결과를 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param requestId VP 요청 ID
     * @return 금융사 VP 요청 상세 응답
     */
    @Operation(
            summary = "금융사 VP 검증 요청 상세 조회",
            description = "금융사 VP 검증 요청 상세와 저장된 검증 결과를 조회합니다. Core를 직접 호출하지 않고 backend DB에 저장된 상태와 결과만 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "금융사 VP 요청 상세 응답",
            content = @Content(schema = @Schema(implementation = FinanceVpRequestDetailResponse.class))
    )
    @GetMapping("/vp-requests/{requestId}")
    public CommonResponse<FinanceVpRequestDetailResponse> getVpRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable String requestId // VP 요청 ID
    ) {
        return CommonResponseFactory.success(verifierVpService.getFinanceVpRequest(userDetails, requestId));
    }
}
