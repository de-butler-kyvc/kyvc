package com.kyvc.backendadmin.domain.document.controller;

import com.kyvc.backendadmin.domain.document.application.DocumentDeleteRequestService;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestActionResponse;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestApproveRequest;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestListResponse;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestRejectRequest;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestSearchRequest;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 원본서류 삭제 요청 관리자 API
 */
@Tag(name = "Document Delete Request Admin", description = "원본서류 삭제 요청 조회 및 승인/반려 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/document-delete-requests")
public class DocumentDeleteRequestController {

    private final DocumentDeleteRequestService documentDeleteRequestService;

    /**
     * 원본서류 삭제 요청 목록 조회
     *
     * @param status 삭제 요청 상태
     * @param keyword 검색어
     * @param kycId KYC 신청 ID
     * @param corporateId 법인 ID
     * @param documentId 문서 ID
     * @param requesterId 요청 사용자 ID
     * @param startDate 요청 시작일
     * @param endDate 요청 종료일
     * @param fromDate 기존 요청 시작일
     * @param toDate 기존 요청 종료일
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 원본서류 삭제 요청 목록
     */
    @Operation(summary = "원본서류 삭제 요청 목록 조회")
    @GetMapping
    public CommonResponse<DocumentDeleteRequestListResponse> search(
            @Parameter(description = "삭제 요청 상태", example = "REQUESTED")
            @RequestParam(required = false) String status,
            @Parameter(description = "법인명, 파일명, 문서 유형, 이메일 검색어")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "KYC 신청 ID", example = "10")
            @RequestParam(required = false) Long kycId,
            @Parameter(description = "법인 ID", example = "30")
            @RequestParam(required = false) Long corporateId,
            @Parameter(description = "문서 ID", example = "20")
            @RequestParam(required = false) Long documentId,
            @Parameter(description = "요청 사용자 ID", example = "40")
            @RequestParam(required = false) Long requesterId,
            @Parameter(description = "요청 시작일", example = "2026-05-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "요청 종료일", example = "2026-05-31")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "기존 요청 시작일", hidden = true)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "기존 요청 종료일", hidden = true)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate toDate,
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        LocalDate effectiveStartDate = startDate == null ? fromDate : startDate;
        LocalDate effectiveEndDate = endDate == null ? toDate : endDate;
        return CommonResponseFactory.success(documentDeleteRequestService.search(
                DocumentDeleteRequestSearchRequest.of(
                        page,
                        size,
                        status,
                        keyword,
                        kycId,
                        corporateId,
                        documentId,
                        requesterId,
                        effectiveStartDate,
                        effectiveEndDate
                )
        ));
    }

    /**
     * 원본서류 삭제 요청 승인
     *
     * @param requestId 삭제 요청 ID
     * @param request 승인 요청 정보
     * @return 처리 결과
     */
    @Operation(summary = "원본서류 삭제 요청 승인")
    @PostMapping("/{requestId}/approve")
    public CommonResponse<DocumentDeleteRequestActionResponse> approve(
            @Parameter(description = "삭제 요청 ID", required = true, example = "1")
            @PathVariable Long requestId,
            @Valid @org.springframework.web.bind.annotation.RequestBody DocumentDeleteRequestApproveRequest request
    ) {
        return CommonResponseFactory.success(documentDeleteRequestService.approve(requestId, request));
    }

    /**
     * 원본서류 삭제 요청 반려
     *
     * @param requestId 삭제 요청 ID
     * @param request 반려 요청 정보
     * @return 처리 결과
     */
    @Operation(summary = "원본서류 삭제 요청 반려")
    @PostMapping("/{requestId}/reject")
    public CommonResponse<DocumentDeleteRequestActionResponse> reject(
            @Parameter(description = "삭제 요청 ID", required = true, example = "1")
            @PathVariable Long requestId,
            @Valid @org.springframework.web.bind.annotation.RequestBody DocumentDeleteRequestRejectRequest request
    ) {
        return CommonResponseFactory.success(documentDeleteRequestService.reject(requestId, request));
    }
}
