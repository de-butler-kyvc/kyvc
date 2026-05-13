package com.kyvc.backend.domain.document.controller;

import com.kyvc.backend.domain.document.application.KycDocumentService;
import com.kyvc.backend.domain.document.dto.DocumentDeleteResponse;
import com.kyvc.backend.domain.document.dto.DocumentPreviewResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 사용자 KYC 문서 Alias API Controller
 */
@RestController
@RequestMapping("/api/user/kyc/applications/{kycId}/documents")
@RequiredArgsConstructor
@Tag(name = "KYC 신청 / 서류", description = "KYC 신청, 필수서류, 문서 업로드, 제출, 완료 및 Credential Offer API")
public class UserKycDocumentAliasController {

    private final KycDocumentService kycDocumentService;

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
            description = "사용자 KYC 문서 미리보기 URL 조회"
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
     * 문서 미리보기 파일 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @param documentId 문서 ID
     * @param expiresAt URL 만료 일시
     * @return 문서 미리보기 파일
     */
    @Operation(
            summary = "문서 미리보기 파일 조회",
            description = "사용자 KYC 문서 미리보기 파일 조회"
    )
    @ApiResponse(
            responseCode = "200",
            description = "문서 미리보기 파일 반환",
            content = @Content(schema = @Schema(type = "string", format = "binary"))
    )
    @GetMapping("/{documentId}/preview-content")
    public ResponseEntity<Resource> getPreviewContent(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId, // KYC 신청 ID
            @Parameter(description = "문서 ID", example = "1")
            @PathVariable Long documentId, // 문서 ID
            @Parameter(description = "URL 만료 일시", example = "2026-05-05T12:10:00")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt // URL 만료 일시
    ) {
        KycDocumentService.DocumentPreviewContent previewContent = kycDocumentService.getDocumentPreviewContent(
                getAuthenticatedUserId(userDetails),
                kycId,
                documentId,
                expiresAt
        );

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
            description = "사용자 KYC DRAFT 문서 삭제 처리"
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
