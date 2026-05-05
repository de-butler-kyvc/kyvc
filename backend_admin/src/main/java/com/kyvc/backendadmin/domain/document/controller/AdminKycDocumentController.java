package com.kyvc.backendadmin.domain.document.controller;

import com.kyvc.backendadmin.domain.document.application.AdminKycDocumentService;
import com.kyvc.backendadmin.domain.document.dto.AdminKycDocumentListResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminKycDocumentPreviewResponse;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KYC 제출 문서 목록 및 미리보기 API를 담당합니다.
 *
 * <p>백엔드 관리자가 KYC 신청에 제출된 문서 목록을 조회하고, 특정 문서의
 * 만료 시간이 있는 미리보기 URL을 발급받을 수 있는 엔드포인트를 제공합니다.</p>
 */
@Tag(name = "KYC Document Admin", description = "백엔드 관리자 KYC 제출 문서 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/kyc/applications/{kycId}/documents")
public class AdminKycDocumentController {

    private final AdminKycDocumentService adminKycDocumentService;

    /**
     * KYC 제출 문서 목록을 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return KYC 제출 문서 목록 응답
     */
    @Operation(summary = "KYC 제출 문서 목록 조회", description = "KYC 신청 존재 여부를 확인한 뒤 kyc_documents.kyc_id 기준으로 제출 문서 목록을 조회합니다.")
    @ApiResponse(responseCode = "404", description = "KYC 신청이 없는 경우")
    @GetMapping
    public CommonResponse<AdminKycDocumentListResponse> getDocuments(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId
    ) {
        return CommonResponseFactory.success(adminKycDocumentService.getDocuments(kycId));
    }

    /**
     * KYC 제출 문서 미리보기 URL을 생성합니다.
     *
     * @param kycId KYC 신청 ID
     * @param documentId 문서 ID
     * @return KYC 제출 문서 미리보기 응답
     */
    @Operation(summary = "KYC 제출 문서 미리보기", description = "KYC와 문서 존재 여부, 문서 소속을 검증한 뒤 원본 파일 경로를 노출하지 않는 미리보기 URL을 생성합니다.")
    @ApiResponse(responseCode = "404", description = "KYC 신청 또는 문서가 없는 경우")
    @ApiResponse(responseCode = "403", description = "문서가 해당 KYC에 속하지 않거나 파일 접근 권한이 없는 경우")
    @GetMapping("/{documentId}/preview")
    public CommonResponse<AdminKycDocumentPreviewResponse> createPreview(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId,
            @Parameter(description = "문서 ID", required = true)
            @PathVariable Long documentId
    ) {
        return CommonResponseFactory.success(adminKycDocumentService.createPreview(kycId, documentId));
    }
}
