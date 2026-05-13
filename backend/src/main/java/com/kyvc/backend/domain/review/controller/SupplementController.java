package com.kyvc.backend.domain.review.controller;

import com.kyvc.backend.domain.review.application.SupplementService;
import com.kyvc.backend.domain.review.dto.SupplementDetailResponse;
import com.kyvc.backend.domain.review.dto.SupplementDocumentResponse;
import com.kyvc.backend.domain.review.dto.SupplementDocumentUploadRequest;
import com.kyvc.backend.domain.review.dto.SupplementListResponse;
import com.kyvc.backend.domain.review.dto.SupplementSubmitRequest;
import com.kyvc.backend.domain.review.dto.SupplementSubmitResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보완요청 API Controller
 */
@RestController
@RequestMapping("/api/corporate/kyc/applications/{kycId}/supplements")
@RequiredArgsConstructor
@Tag(name = "KYC 신청 / 서류", description = "KYC 신청, 필수서류, 문서 업로드, 제출, 완료 및 Credential Offer API")
public class SupplementController {

    private final SupplementService supplementService;

    /**
     * 보완요청 목록 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 요청 ID
     * @return 보완요청 목록 응답
     */
    @Operation(
            summary = "보완요청 목록 조회",
            description = "로그인 사용자의 KYC 요청에 등록된 보완요청 목록을 최신순으로 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "보완요청 목록 반환",
            content = @Content(schema = @Schema(implementation = SupplementListResponse.class))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<SupplementListResponse>> getSupplements(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 요청 ID", example = "1")
            @PathVariable Long kycId // KYC 요청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                supplementService.getSupplements(getAuthenticatedUserId(userDetails), kycId)
        ));
    }

    /**
     * 보완요청 상세 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 요청 ID
     * @param supplementId 보완요청 ID
     * @return 보완요청 상세 응답
     */
    @Operation(
            summary = "보완요청 상세 조회",
            description = "로그인 사용자의 KYC 요청에 속한 특정 보완요청 상세를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "보완요청 상세 반환",
            content = @Content(schema = @Schema(implementation = SupplementDetailResponse.class))
    )
    @GetMapping("/{supplementId}")
    public ResponseEntity<CommonResponse<SupplementDetailResponse>> getSupplement(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 요청 ID", example = "1")
            @PathVariable Long kycId, // KYC 요청 ID
            @Parameter(description = "보완요청 ID", example = "1")
            @PathVariable Long supplementId // 보완요청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                supplementService.getSupplement(getAuthenticatedUserId(userDetails), kycId, supplementId)
        ));
    }

    /**
     * 보완 문서 업로드
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 요청 ID
     * @param supplementId 보완요청 ID
     * @param request 보완 문서 업로드 요청
     * @return 보완 문서 응답
     */
    @Operation(
            summary = "보완 문서 업로드",
            description = "보완요청에 포함된 문서 유형만 업로드하고 기존 KYC 문서 저장 로직을 재사용합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "업로드된 보완 문서 반환",
            content = @Content(schema = @Schema(implementation = SupplementDocumentResponse.class))
    )
    @PostMapping(value = "/{supplementId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<SupplementDocumentResponse>> uploadSupplementDocument(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 요청 ID", example = "1")
            @PathVariable Long kycId, // KYC 요청 ID
            @Parameter(description = "보완요청 ID", example = "1")
            @PathVariable Long supplementId, // 보완요청 ID
            @Valid @ModelAttribute SupplementDocumentUploadRequest request // 보완 문서 업로드 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                supplementService.uploadSupplementDocument(
                        getAuthenticatedUserId(userDetails),
                        kycId,
                        supplementId,
                        request
                )
        ));
    }

    /**
     * 보완 제출
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 요청 ID
     * @param supplementId 보완요청 ID
     * @param request 보완 제출 요청
     * @return 보완 제출 응답
     */
    @Operation(
            summary = "보완 제출",
            description = "필수 보완서류 충족 여부를 검증한 뒤 보완요청과 KYC 상태를 제출 완료 상태로 변경합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "보완 제출 결과 반환",
            content = @Content(schema = @Schema(implementation = SupplementSubmitResponse.class))
    )
    @PostMapping("/{supplementId}/submit")
    public ResponseEntity<CommonResponse<SupplementSubmitResponse>> submitSupplement(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 요청 ID", example = "1")
            @PathVariable Long kycId, // KYC 요청 ID
            @Parameter(description = "보완요청 ID", example = "1")
            @PathVariable Long supplementId, // 보완요청 ID
            @RequestBody(required = false) SupplementSubmitRequest request // 보완 제출 요청
    ) {
        SupplementSubmitRequest submitRequest = request == null
                ? new SupplementSubmitRequest(null)
                : request; // null 요청 본문 보정

        return ResponseEntity.ok(CommonResponseFactory.success(
                supplementService.submitSupplement(
                        getAuthenticatedUserId(userDetails),
                        kycId,
                        supplementId,
                        submitRequest
                )
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
