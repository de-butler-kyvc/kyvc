package com.kyvc.backend.domain.finance.controller;

import com.kyvc.backend.domain.finance.application.FinanceCorporateCustomerService;
import com.kyvc.backend.domain.finance.dto.FinanceCorporateCustomerLinkRequest;
import com.kyvc.backend.domain.finance.dto.FinanceCorporateCustomerLinkResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 금융사 법인 고객 연결 API Controller
 */
@RestController
@RequestMapping("/api/finance/corporate-customers")
@RequiredArgsConstructor
@Tag(name = "Finance Corporate Customer", description = "금융사 법인 고객 연결 API")
public class FinanceCorporateCustomerController {

    private final FinanceCorporateCustomerService financeCorporateCustomerService;

    /**
     * 금융사 법인 고객 연결 생성
     *
     * @param userDetails 인증 사용자 정보
     * @param request 금융사 법인 고객 연결 요청
     * @return 금융사 법인 고객 연결 응답
     */
    @Operation(
            summary = "금융사 법인 고객 연결 생성",
            description = "금융사 고객번호와 KYvC 법인을 연결합니다."
    )
    @ApiResponse(
            responseCode = "201",
            description = "금융사 법인 고객 연결 생성 결과",
            content = @Content(schema = @Schema(implementation = FinanceCorporateCustomerLinkResponse.class))
    )
    @PostMapping("/link")
    public ResponseEntity<CommonResponse<FinanceCorporateCustomerLinkResponse>> linkCorporateCustomer(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "금융사 법인 고객 연결 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FinanceCorporateCustomerLinkRequest.class))
            )
            @Valid @RequestBody FinanceCorporateCustomerLinkRequest request // 금융사 법인 고객 연결 요청
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponseFactory.success(
                        financeCorporateCustomerService.linkCorporateCustomer(userDetails, request)
                ));
    }
}
