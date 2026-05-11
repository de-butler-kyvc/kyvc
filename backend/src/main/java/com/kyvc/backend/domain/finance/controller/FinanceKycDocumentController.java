package com.kyvc.backend.domain.finance.controller;

import com.kyvc.backend.domain.finance.application.FinanceKycDocumentService;
import com.kyvc.backend.domain.finance.dto.FinanceKycDocumentListResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycDocumentUploadRequest;
import com.kyvc.backend.domain.finance.dto.FinanceKycDocumentUploadResponse;
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
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 금융사 방문 KYC 문서 API Controller
 */
@RestController
@RequestMapping("/api/finance/kyc/applications/{kycId}/documents")
@RequiredArgsConstructor
@Tag(name = "Finance Visit KYC Documents", description = "금융사 방문 KYC 서류 API")
public class FinanceKycDocumentController {

    private final FinanceKycDocumentService financeKycDocumentService;

    /**
     * 금융사 방문 KYC 문서 업로드
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @param request 문서 업로드 요청
     * @return 문서 업로드 응답
     */
    @Operation(
            summary = "금융사 방문 KYC 문서 업로드",
            description = "금융사 직원이 본인 범위의 방문 KYC 신청에 제출서류를 업로드합니다."
    )
    @ApiResponse(
            responseCode = "201",
            description = "문서 업로드 결과",
            content = @Content(schema = @Schema(implementation = FinanceKycDocumentUploadResponse.class))
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<FinanceKycDocumentUploadResponse>> uploadDocument(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId, // KYC 신청 ID
            @Valid @ModelAttribute FinanceKycDocumentUploadRequest request // 문서 업로드 요청
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponseFactory.success(
                        financeKycDocumentService.uploadDocument(userDetails, kycId, request)
                ));
    }

    /**
     * 금융사 방문 KYC 문서 목록 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return 문서 목록 응답
     */
    @Operation(
            summary = "금융사 방문 KYC 문서 목록 조회",
            description = "금융사 직원이 본인 범위의 방문 KYC 제출서류 목록을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "문서 목록 반환",
            content = @Content(schema = @Schema(implementation = FinanceKycDocumentListResponse.class))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<FinanceKycDocumentListResponse>> getDocuments(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                financeKycDocumentService.getDocuments(userDetails, kycId)
        ));
    }

    /**
     * 금융사 방문 KYC 문서 미리보기
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @param documentId 문서 ID
     * @return 문서 미리보기 파일
     */
    @Operation(
            summary = "금융사 방문 KYC 문서 미리보기",
            description = "금융사 직원이 본인 범위의 방문 KYC 제출서류 파일을 미리보기합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "문서 미리보기 파일 반환",
            content = @Content(schema = @Schema(type = "string", format = "binary"))
    )
    @GetMapping("/{documentId}/preview")
    public ResponseEntity<Resource> getPreview(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId, // KYC 신청 ID
            @Parameter(description = "문서 ID", example = "1")
            @PathVariable Long documentId // 문서 ID
    ) {
        FinanceKycDocumentService.DocumentPreviewContent previewContent =
                financeKycDocumentService.getPreviewContent(userDetails, kycId, documentId);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(previewContent.mimeType()))
                .contentLength(previewContent.fileSize())
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(previewContent.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(previewContent.resource());
    }

    // MIME 타입 변환
    private MediaType resolveMediaType(
            String mimeType // MIME 타입
    ) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
