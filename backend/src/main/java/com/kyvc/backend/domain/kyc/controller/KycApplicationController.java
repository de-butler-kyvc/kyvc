package com.kyvc.backend.domain.kyc.controller;

import com.kyvc.backend.domain.kyc.application.KycApplicationService;
import com.kyvc.backend.domain.kyc.dto.DocumentStoreOptionRequest;
import com.kyvc.backend.domain.kyc.dto.KycApplicationResponse;
import com.kyvc.backend.domain.kyc.dto.KycCorporateTypeRequest;
import com.kyvc.backend.domain.kyc.dto.KycStartRequest;
import com.kyvc.backend.domain.kyc.dto.KycStatusResponse;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * KYC 신청 API Controller
 */
@RestController
@RequestMapping("/api/corporate/kyc/applications")
@RequiredArgsConstructor
@Tag(name = "KYC 신청", description = "KYC 신청 초안 생성, 법인 유형, 상태, 원본서류 저장 옵션 API")
public class KycApplicationController {

    private final KycApplicationService kycApplicationService;

    /**
     * KYC 신청 시작
     *
     * @param userDetails 인증 사용자 정보
     * @param request KYC 신청 시작 요청
     * @return KYC 신청 응답
     */
    @Operation(
            summary = "KYC 신청 시작",
            description = "로그인 사용자의 법인정보 기준으로 KYC 신청 초안을 생성합니다. 입력값은 법인 유형 코드입니다."
    )
    @ApiResponse(
            responseCode = "201",
            description = "생성된 KYC 신청 ID, 법인 ID, 신청 사용자 ID, 법인 유형, KYC 상태 반환",
            content = @Content(schema = @Schema(implementation = KycApplicationResponse.class))
    )
    @PostMapping
    public ResponseEntity<CommonResponse<KycApplicationResponse>> startKyc(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "KYC 신청 시작 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = KycStartRequest.class))
            )
            @Valid @RequestBody KycStartRequest request // KYC 신청 시작 요청
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponseFactory.success(
                        kycApplicationService.startKyc(getAuthenticatedUserId(userDetails), request)
                ));
    }

    /**
     * KYC 법인 유형 선택
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @param request KYC 법인 유형 변경 요청
     * @return KYC 신청 응답
     */
    @Operation(
            summary = "KYC 법인 유형 선택",
            description = "KYC 신청 초안의 법인 유형을 변경합니다. 입력값은 KYC 신청 ID와 법인 유형 코드입니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "변경된 KYC 신청 ID, 법인 유형, KYC 상태 반환",
            content = @Content(schema = @Schema(implementation = KycApplicationResponse.class))
    )
    @PutMapping("/{kycId}/corporate-type")
    public ResponseEntity<CommonResponse<KycApplicationResponse>> changeCorporateType(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId, // KYC 신청 ID
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "KYC 법인 유형 변경 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = KycCorporateTypeRequest.class))
            )
            @Valid @RequestBody KycCorporateTypeRequest request // KYC 법인 유형 변경 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycApplicationService.changeCorporateType(getAuthenticatedUserId(userDetails), kycId, request)
        ));
    }

    /**
     * KYC 신청 상세 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return KYC 신청 응답
     */
    @Operation(
            summary = "KYC 신청 상세 조회",
            description = "로그인 사용자 소유의 KYC 신청 상세 정보를 조회합니다. 입력값은 KYC 신청 ID입니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "KYC 신청 ID, 법인 ID, 신청 사용자 ID, 법인 유형, 상태, 원본서류 저장 옵션 반환",
            content = @Content(schema = @Schema(implementation = KycApplicationResponse.class))
    )
    @GetMapping("/{kycId}")
    public ResponseEntity<CommonResponse<KycApplicationResponse>> getKycApplication(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycApplicationService.getKycApplication(getAuthenticatedUserId(userDetails), kycId)
        ));
    }

    /**
     * KYC 진행상태 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return KYC 진행상태 응답
     */
    @Operation(
            summary = "KYC 진행상태 조회",
            description = "로그인 사용자 소유의 KYC 신청 진행상태를 조회합니다. 입력값은 KYC 신청 ID입니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "KYC 신청 ID, KYC 상태, 법인 유형, 원본서류 저장 옵션, 제출일시 반환",
            content = @Content(schema = @Schema(implementation = KycStatusResponse.class))
    )
    @GetMapping("/{kycId}/status")
    public ResponseEntity<CommonResponse<KycStatusResponse>> getKycStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycApplicationService.getKycStatus(getAuthenticatedUserId(userDetails), kycId)
        ));
    }

    /**
     * 원본서류 저장 옵션 선택
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @param request 원본서류 저장 옵션 변경 요청
     * @return KYC 신청 응답
     */
    @Operation(
            summary = "원본서류 저장 옵션 선택",
            description = "KYC 신청 초안의 원본서류 저장 옵션을 선택합니다. 입력값은 KYC 신청 ID와 저장 옵션입니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "변경된 KYC 신청 ID, 원본서류 저장 옵션, KYC 상태 반환",
            content = @Content(schema = @Schema(implementation = KycApplicationResponse.class))
    )
    @PutMapping("/{kycId}/document-store-option")
    public ResponseEntity<CommonResponse<KycApplicationResponse>> changeDocumentStoreOption(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId, // KYC 신청 ID
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "원본서류 저장 옵션 변경 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DocumentStoreOptionRequest.class))
            )
            @Valid @RequestBody DocumentStoreOptionRequest request // 원본서류 저장 옵션 변경 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycApplicationService.changeDocumentStoreOption(getAuthenticatedUserId(userDetails), kycId, request)
        ));
    }

    // 인증 사용자 ID 조회
    private Long getAuthenticatedUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
