package com.kyvc.backend.domain.document.controller;

import com.kyvc.backend.domain.document.application.KycDocumentService;
import com.kyvc.backend.domain.document.dto.DocumentDeleteResponse;
import com.kyvc.backend.domain.document.dto.DocumentPreviewResponse;
import com.kyvc.backend.domain.document.dto.KycDocumentResponse;
import com.kyvc.backend.domain.document.dto.KycDocumentUploadRequest;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * KYC 문서 API Controller
 */
@RestController
@RequestMapping("/api/corporate/kyc/applications/{kycId}/documents")
@RequiredArgsConstructor
@Tag(name = "KYC 신청 / 서류", description = "KYC 신청, 필수서류, 문서 업로드, 제출, 완료 및 Credential Offer API")
public class KycDocumentController {

    private final KycDocumentService kycDocumentService;

    /**
     * KYC 서류 업로드
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @param request KYC 문서 업로드 요청
     * @return KYC 문서 응답
     */
    @Operation(
            summary = "KYC 서류 업로드",
            description = "KYC 신청 건에 서류 파일을 업로드합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "KYC 문서 응답 반환",
            content = @Content(schema = @Schema(implementation = KycDocumentResponse.class))
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<KycDocumentResponse>> uploadDocument(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId, // KYC 신청 ID
            @Valid @ModelAttribute KycDocumentUploadRequest request // KYC 문서 업로드 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycDocumentService.uploadDocument(getAuthenticatedUserId(userDetails), kycId, request)
        ));
    }

    /**
     * 업로드 문서 목록 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return KYC 문서 목록 응답
     */
    @Operation(
            summary = "업로드 문서 목록 조회",
            description = "KYC 신청 건에 업로드된 서류 목록을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "문서 목록 반환",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = KycDocumentResponse.class)))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<List<KycDocumentResponse>>> getDocuments(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycDocumentService.getDocuments(getAuthenticatedUserId(userDetails), kycId)
        ));
    }

    /**
     * 업로드 문서 상세 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @param documentId 문서 ID
     * @return KYC 문서 응답
     */
    @Operation(
            summary = "업로드 문서 상세 조회",
            description = "KYC 신청 건에 업로드된 특정 서류의 상세 정보를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "문서 상세 응답 반환",
            content = @Content(schema = @Schema(implementation = KycDocumentResponse.class))
    )
    @GetMapping("/{documentId}")
    public ResponseEntity<CommonResponse<KycDocumentResponse>> getDocument(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId, // KYC 신청 ID
            @Parameter(description = "문서 ID", example = "1")
            @PathVariable Long documentId // 문서 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycDocumentService.getDocument(getAuthenticatedUserId(userDetails), kycId, documentId)
        ));
    }

    /**
     * 문서 미리보기 URL 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @param documentId 문서 ID
     * @return 문서 미리보기 응답
     */
    @Operation(
            summary = "문서 미리보기 URL 조회",
            description = "KYC 문서 미리보기 URL 정보를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "문서 미리보기 응답 반환",
            content = @Content(schema = @Schema(implementation = DocumentPreviewResponse.class))
    )
    @GetMapping("/{documentId}/preview")
    public ResponseEntity<CommonResponse<DocumentPreviewResponse>> getPreview(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId, // KYC 신청 ID
            @Parameter(description = "문서 ID", example = "1")
            @PathVariable Long documentId // 문서 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycDocumentService.getDocumentPreview(getAuthenticatedUserId(userDetails), kycId, documentId)
        ));
    }

    /**
     * 제출 전 문서 삭제
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @param documentId 문서 ID
     * @return 문서 삭제 응답
     */
    @Operation(
            summary = "제출 전 문서 삭제",
            description = "DRAFT 상태 KYC 문서 삭제 처리"
    )
    @ApiResponse(
            responseCode = "200",
            description = "문서 삭제 응답 반환",
            content = @Content(schema = @Schema(implementation = DocumentDeleteResponse.class))
    )
    @DeleteMapping("/{documentId}")
    public ResponseEntity<CommonResponse<DocumentDeleteResponse>> deleteDocument(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId, // KYC 신청 ID
            @Parameter(description = "문서 ID", example = "1")
            @PathVariable Long documentId // 문서 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycDocumentService.deleteDocument(getAuthenticatedUserId(userDetails), kycId, documentId)
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
