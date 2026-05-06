package com.kyvc.backend.domain.document.controller;

import com.kyvc.backend.domain.document.application.RequiredDocumentService;
import com.kyvc.backend.domain.document.dto.RequiredDocumentResponse;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 필수서류 안내 API Controller
 */
@RestController
@RequestMapping("/api/corporate/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC 필수서류", description = "KYC 법인 유형별 필수서류 안내 API")
public class RequiredDocumentController {

    private final RequiredDocumentService requiredDocumentService;

    /**
     * 법인 유형별 필수서류 안내 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param corporateTypeCode 법인 유형 코드
     * @return 필수서류 안내 목록 응답
     */
    @Operation(
            summary = "법인 유형별 필수서류 안내 조회",
            description = "법인 유형 코드 기준으로 KYC 신청에 필요한 필수서류 5개를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "문서 유형 코드, 문서명, 필수 여부, 업로드 여부, 안내 문구, 허용 확장자, 최대 파일 크기 반환",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequiredDocumentResponse.class)))
    )
    @GetMapping("/required-documents")
    public ResponseEntity<CommonResponse<List<RequiredDocumentResponse>>> getRequiredDocuments(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "법인 유형 코드", example = "CORPORATION")
            @RequestParam String corporateTypeCode // 법인 유형 코드
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                requiredDocumentService.getRequiredDocuments(getAuthenticatedUserId(userDetails), corporateTypeCode)
        ));
    }

    /**
     * 신청 건 기준 필수서류 및 업로드 여부 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return 필수서류 안내 목록 응답
     */
    @Operation(
            summary = "신청 건 기준 필수서류 및 업로드 여부 조회",
            description = "KYC 신청 건의 법인 유형 기준 필수서류와 현재 업로드 여부를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "문서 유형 코드, 문서명, 필수 여부, 업로드 여부, 안내 문구, 허용 확장자, 최대 파일 크기 반환",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequiredDocumentResponse.class)))
    )
    @GetMapping("/applications/{kycId}/required-documents")
    public ResponseEntity<CommonResponse<List<RequiredDocumentResponse>>> getRequiredDocumentsByKyc(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                requiredDocumentService.getRequiredDocumentsByKyc(getAuthenticatedUserId(userDetails), kycId)
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
