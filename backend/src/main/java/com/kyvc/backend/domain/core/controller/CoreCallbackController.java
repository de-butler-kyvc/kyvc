package com.kyvc.backend.domain.core.controller;

import com.kyvc.backend.domain.core.application.CoreCallbackService;
import com.kyvc.backend.domain.core.dto.CoreAiReviewCallbackRequest;
import com.kyvc.backend.domain.core.dto.CoreCallbackResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceCallbackRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationCallbackRequest;
import com.kyvc.backend.domain.core.dto.CoreXrplTransactionCallbackRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Core Callback 수신 API Controller
 */
@RestController
@RequestMapping("/api/internal/core")
@RequiredArgsConstructor
@Tag(name = "Core 연동 / Callback", description = "Core 내부 헬스체크, Issuer 정책, AI/VC/XRPL/VP callback 및 개발용 E2E API")
public class CoreCallbackController {

    private final CoreCallbackService coreCallbackService;

    /**
     * AI 심사 Callback 수신
     *
     * @param coreRequestId Core 요청 ID
     * @param callbackRequest AI 심사 Callback 요청
     * @return AI 심사 Callback 처리 응답
     */
    @Operation(
            summary = "AI 심사 Callback 수신",
            description = "AI 심사 Callback 수신 후 CoreRequest 상태 반영"
    )
    @ApiResponse(
            responseCode = "200",
            description = "AI 심사 Callback 처리 응답 반환",
            content = @Content(schema = @Schema(implementation = CoreCallbackResponse.class))
    )
    @PostMapping("/ai-reviews/{coreRequestId}/callback")
    public ResponseEntity<CommonResponse<CoreCallbackResponse>> aiReviewCallback(
            @Parameter(description = "Core 요청 ID", example = "2a3a5630-602a-41cc-8f11-7ef5114c2e30")
            @PathVariable String coreRequestId, // Core 요청 ID
            @RequestBody CoreAiReviewCallbackRequest callbackRequest // AI 심사 Callback 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                coreCallbackService.processAiReviewCallback(coreRequestId, callbackRequest)
        ));
    }

    /**
     * VC 발급 Callback 수신
     *
     * @param coreRequestId Core 요청 ID
     * @param callbackRequest VC 발급 Callback 요청
     * @return VC 발급 Callback 처리 응답
     */
    @Operation(
            summary = "VC 발급 Callback 수신",
            description = "VC 발급 Callback 수신 후 CoreRequest 상태 반영"
    )
    @ApiResponse(
            responseCode = "200",
            description = "VC 발급 Callback 처리 응답 반환",
            content = @Content(schema = @Schema(implementation = CoreCallbackResponse.class))
    )
    @PostMapping("/vc-issuances/{coreRequestId}/callback")
    public ResponseEntity<CommonResponse<CoreCallbackResponse>> vcIssuanceCallback(
            @Parameter(description = "Core 요청 ID", example = "fcf03404-f0eb-43f9-b56a-a5f3b8fdd7de")
            @PathVariable String coreRequestId, // Core 요청 ID
            @RequestBody CoreVcIssuanceCallbackRequest callbackRequest // VC 발급 Callback 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                coreCallbackService.processVcIssuanceCallback(coreRequestId, callbackRequest)
        ));
    }

    /**
     * VP 검증 Callback 수신
     *
     * @param coreRequestId Core 요청 ID
     * @param callbackRequest VP 검증 Callback 요청
     * @return VP 검증 Callback 처리 응답
     */
    @Operation(
            summary = "VP 검증 Callback 수신",
            description = "VP 검증 Callback 수신 후 CoreRequest 상태 반영"
    )
    @ApiResponse(
            responseCode = "200",
            description = "VP 검증 Callback 처리 응답 반환",
            content = @Content(schema = @Schema(implementation = CoreCallbackResponse.class))
    )
    @PostMapping("/vp-verifications/{coreRequestId}/callback")
    public ResponseEntity<CommonResponse<CoreCallbackResponse>> vpVerificationCallback(
            @Parameter(description = "Core 요청 ID", example = "36ad38f0-d3bd-41a1-8c4e-079dcaec3075")
            @PathVariable String coreRequestId, // Core 요청 ID
            @RequestBody CoreVpVerificationCallbackRequest callbackRequest // VP 검증 Callback 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                coreCallbackService.processVpVerificationCallback(coreRequestId, callbackRequest)
        ));
    }

    /**
     * XRPL 트랜잭션 Callback 수신
     *
     * @param coreRequestId Core 요청 ID
     * @param callbackRequest XRPL Callback 요청
     * @return XRPL Callback 처리 응답
     */
    @Operation(
            summary = "XRPL 트랜잭션 Callback 수신",
            description = "XRPL 트랜잭션 Callback 수신 후 CoreRequest 상태 반영"
    )
    @ApiResponse(
            responseCode = "200",
            description = "XRPL Callback 처리 응답 반환",
            content = @Content(schema = @Schema(implementation = CoreCallbackResponse.class))
    )
    @PostMapping("/xrpl-transactions/{coreRequestId}/callback")
    public ResponseEntity<CommonResponse<CoreCallbackResponse>> xrplTransactionCallback(
            @Parameter(description = "Core 요청 ID", example = "42e42720-66ed-48f1-a4b2-f6ea3ff5594c")
            @PathVariable String coreRequestId, // Core 요청 ID
            @RequestBody CoreXrplTransactionCallbackRequest callbackRequest // XRPL Callback 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                coreCallbackService.processXrplTransactionCallback(coreRequestId, callbackRequest)
        ));
    }
}
