package com.kyvc.backendadmin.domain.document.controller;

import com.kyvc.backendadmin.domain.document.application.AdminDocumentDeleteRequestService;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteApproveRequest;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteProcessResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRejectRequest;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestDetailResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestListResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestSearchRequest;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "문서 삭제 요청 관리", description = "백오피스 문서 삭제 요청 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/document-delete-requests")
public class AdminDocumentDeleteRequestController {

    private final AdminDocumentDeleteRequestService service;

    /**
     * 문서 삭제 요청 목록을 조회한다.
     *
     * @param status 요청 상태 코드
     * @param keyword 검색어
     * @param fromDate 요청 시작일
     * @param toDate 요청 종료일
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 문서 삭제 요청 목록
     */
    @Operation(
            summary = "문서 삭제 요청 목록 조회",
            description = "요청 상태, 검색어, 요청일 기간, 페이징 조건으로 문서 삭제 요청 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "문서 삭제 요청 목록 조회 성공")
    @GetMapping
    public CommonResponse<AdminDocumentDeleteRequestListResponse> getDeleteRequests(
            @Parameter(description = "요청 상태 코드", example = "REQUESTED")
            @RequestParam(required = false) String status,
            @Parameter(description = "법인명, 파일명, 요청자 이메일 검색어", example = "KYVC")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "요청 시작일", example = "2026-05-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "요청 종료일", example = "2026-05-31")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate toDate,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return CommonResponseFactory.success(service.getDeleteRequests(
                new AdminDocumentDeleteRequestSearchRequest(status, keyword, fromDate, toDate, page, size)
        ));
    }

    /**
     * 문서 삭제 요청 상세 정보를 조회한다.
     *
     * @param requestId 문서 삭제 요청 ID
     * @return 문서 삭제 요청 상세 정보
     */
    @Operation(
            summary = "문서 삭제 요청 상세 조회",
            description = "문서 삭제 요청의 요청자, 처리자, 법인, 문서 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "문서 삭제 요청 상세 조회 성공")
    @ApiResponse(responseCode = "404", description = "문서 삭제 요청을 찾을 수 없음")
    @GetMapping("/{requestId}")
    public CommonResponse<AdminDocumentDeleteRequestDetailResponse> getDeleteRequestDetail(
            @Parameter(description = "문서 삭제 요청 ID", required = true, example = "1")
            @PathVariable Long requestId
    ) {
        return CommonResponseFactory.success(service.getDeleteRequestDetail(requestId));
    }

    /**
     * 문서 삭제 요청을 승인한다.
     *
     * @param requestId 문서 삭제 요청 ID
     * @param request 승인 요청
     * @return 승인 처리 결과
     */
    @Operation(
            summary = "문서 삭제 요청 승인",
            description = "REQUESTED 상태의 문서 삭제 요청을 승인하고 대상 KYC 문서 업로드 상태를 DELETED로 변경합니다."
    )
    @ApiResponse(responseCode = "200", description = "문서 삭제 요청 승인 성공")
    @ApiResponse(responseCode = "400", description = "이미 처리된 요청")
    @ApiResponse(responseCode = "404", description = "문서 삭제 요청 또는 대상 문서를 찾을 수 없음")
    @PostMapping("/{requestId}/approve")
    public CommonResponse<AdminDocumentDeleteProcessResponse> approve(
            @Parameter(description = "문서 삭제 요청 ID", required = true, example = "1")
            @PathVariable Long requestId,
            @RequestBody(description = "문서 삭제 요청 승인 본문", required = true)
            @org.springframework.web.bind.annotation.RequestBody(required = false)
            AdminDocumentDeleteApproveRequest request
    ) {
        return CommonResponseFactory.success(service.approve(
                requestId,
                request == null ? new AdminDocumentDeleteApproveRequest(null) : request
        ));
    }

    /**
     * 문서 삭제 요청을 반려한다.
     *
     * @param requestId 문서 삭제 요청 ID
     * @param request 반려 요청
     * @return 반려 처리 결과
     */
    @Operation(
            summary = "문서 삭제 요청 반려",
            description = "REQUESTED 상태의 문서 삭제 요청을 반려하며 대상 KYC 문서 업로드 상태는 변경하지 않습니다."
    )
    @ApiResponse(responseCode = "200", description = "문서 삭제 요청 반려 성공")
    @ApiResponse(responseCode = "400", description = "이미 처리된 요청")
    @ApiResponse(responseCode = "404", description = "문서 삭제 요청 또는 대상 문서를 찾을 수 없음")
    @PostMapping("/{requestId}/reject")
    public CommonResponse<AdminDocumentDeleteProcessResponse> reject(
            @Parameter(description = "문서 삭제 요청 ID", required = true, example = "1")
            @PathVariable Long requestId,
            @RequestBody(description = "문서 삭제 요청 반려 본문", required = true)
            @org.springframework.web.bind.annotation.RequestBody(required = false)
            AdminDocumentDeleteRejectRequest request
    ) {
        return CommonResponseFactory.success(service.reject(
                requestId,
                request == null ? new AdminDocumentDeleteRejectRequest(null) : request
        ));
    }
}
