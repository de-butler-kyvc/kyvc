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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * 원본서류 삭제 요청 관리자 API입니다.
 *
 * <p>관리자가 사용자의 원본서류 삭제 요청을 검색하고, MFA 검증 후 승인 또는 반려할 수 있습니다.</p>
 */
@Tag(name = "Document Delete Request Admin", description = "관리자 원본서류 삭제 요청 조회 및 승인/반려 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/document-delete-requests")
public class DocumentDeleteRequestController {

    private final DocumentDeleteRequestService documentDeleteRequestService;

    /**
     * 원본서류 삭제 요청 목록을 조회합니다.
     *
     * @param status 삭제 요청 상태
     * @param keyword 법인명, 파일명, 문서 유형, 이메일 검색어
     * @param fromDate 삭제 요청 시작일
     * @param toDate 삭제 요청 종료일
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 원본서류 삭제 요청 목록
     */
    @Operation(
            summary = "원본서류 삭제 요청 목록 조회",
            description = "관리자가 사용자의 원본서류 삭제 요청을 상태, 검색어, 요청일 기간 조건으로 조회한다."
    )
    @ApiResponse(responseCode = "200", description = "원본서류 삭제 요청 목록 조회 성공")
    @GetMapping
    public CommonResponse<DocumentDeleteRequestListResponse> search(
            @Parameter(description = "삭제 요청 상태", example = "REQUESTED")
            @RequestParam(required = false) String status,
            @Parameter(description = "법인명, 파일명, 문서 유형, 이메일 검색어")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "삭제 요청 시작일", example = "2026-05-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "삭제 요청 종료일", example = "2026-05-31")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate toDate,
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size
    ) {
        return CommonResponseFactory.success(documentDeleteRequestService.search(
                DocumentDeleteRequestSearchRequest.of(page, size, status, keyword, fromDate, toDate)
        ));
    }

    /**
     * 원본서류 삭제 요청을 승인합니다.
     *
     * @param requestId 삭제 요청 ID
     * @param request 원본서류 삭제 승인 요청 정보
     * @return 원본서류 삭제 요청 처리 결과
     */
    @Operation(
            summary = "원본서류 삭제 요청 승인",
            description = "관리자가 MFA 검증 후 원본서류 삭제 요청을 승인하고 KYC 문서 상태를 DELETED로 변경한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "원본서류 삭제 요청 승인 성공"),
            @ApiResponse(responseCode = "401", description = "MFA 토큰이 유효하지 않은 경우"),
            @ApiResponse(responseCode = "404", description = "삭제 요청 또는 문서를 찾을 수 없는 경우"),
            @ApiResponse(responseCode = "409", description = "이미 처리된 삭제 요청인 경우")
    })
    @PostMapping("/{requestId}/approve")
    public CommonResponse<DocumentDeleteRequestActionResponse> approve(
            @Parameter(description = "삭제 요청 ID", required = true, example = "1")
            @PathVariable Long requestId,
            @RequestBody(
                    description = "원본서류 삭제 승인 요청 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DocumentDeleteRequestApproveRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody DocumentDeleteRequestApproveRequest request
    ) {
        return CommonResponseFactory.success(documentDeleteRequestService.approve(requestId, request));
    }

    /**
     * 원본서류 삭제 요청을 반려합니다.
     *
     * @param requestId 삭제 요청 ID
     * @param request 원본서류 삭제 반려 요청 정보
     * @return 원본서류 삭제 요청 처리 결과
     */
    @Operation(
            summary = "원본서류 삭제 요청 반려",
            description = "관리자가 MFA 검증 후 원본서류 삭제 요청을 반려하고 반려 사유를 저장한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "원본서류 삭제 요청 반려 성공"),
            @ApiResponse(responseCode = "401", description = "MFA 토큰이 유효하지 않은 경우"),
            @ApiResponse(responseCode = "404", description = "삭제 요청 또는 문서를 찾을 수 없는 경우"),
            @ApiResponse(responseCode = "409", description = "이미 처리된 삭제 요청인 경우")
    })
    @PostMapping("/{requestId}/reject")
    public CommonResponse<DocumentDeleteRequestActionResponse> reject(
            @Parameter(description = "삭제 요청 ID", required = true, example = "1")
            @PathVariable Long requestId,
            @RequestBody(
                    description = "원본서류 삭제 반려 요청 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DocumentDeleteRequestRejectRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody DocumentDeleteRequestRejectRequest request
    ) {
        return CommonResponseFactory.success(documentDeleteRequestService.reject(requestId, request));
    }
}
