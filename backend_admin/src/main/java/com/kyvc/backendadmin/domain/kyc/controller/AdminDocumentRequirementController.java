package com.kyvc.backendadmin.domain.kyc.controller;

import com.kyvc.backendadmin.domain.kyc.application.AdminDocumentRequirementService;
import com.kyvc.backendadmin.domain.kyc.dto.AdminDocumentRequirementCreateRequest;
import com.kyvc.backendadmin.domain.kyc.dto.AdminDocumentRequirementListResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminDocumentRequirementResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminDocumentRequirementSearchRequest;
import com.kyvc.backendadmin.domain.kyc.dto.AdminDocumentRequirementUpdateRequest;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 법인 유형별 필수서류 정책 API를 담당합니다.
 *
 * <p>백엔드 관리자가 법인 유형별 필수서류 정책 목록을 조회하고 신규 정책을 등록할 수 있는
 * 엔드포인트를 제공합니다.</p>
 */
@Tag(name = "KYC Policy Admin", description = "백엔드 관리자 KYC 필수서류 정책 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/document-requirements")
public class AdminDocumentRequirementController {

    private final AdminDocumentRequirementService adminDocumentRequirementService;

    /**
     * 필수서류 정책 목록을 검색합니다.
     *
     * <p>page는 0부터 시작하는 페이지 번호, size는 페이지 크기입니다.
     * corporateType은 법인 유형 공통코드, documentType은 문서 유형 공통코드입니다.
     * requiredYn은 필수 여부, enabledYn은 사용 여부를 의미합니다.</p>
     *
     * @param request 필수서류 정책 검색 조건
     * @return 필수서류 정책 목록 응답
     */
    @Operation(summary = "필수서류 정책 목록 조회", description = "page, size, corporateType, documentType, requiredYn, enabledYn 조건으로 필수서류 정책 목록을 조회합니다.")
    @GetMapping
    public CommonResponse<AdminDocumentRequirementListResponse> search(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "법인 유형 공통코드", example = "CORPORATION")
            @RequestParam(required = false) String corporateType,
            @Parameter(description = "문서 유형 공통코드", example = "BUSINESS_REGISTRATION")
            @RequestParam(required = false) String documentType,
            @Parameter(description = "필수 여부(Y/N)", example = "Y")
            @RequestParam(required = false) String requiredYn,
            @Parameter(description = "사용 여부(Y/N)", example = "Y")
            @RequestParam(required = false) String enabledYn
    ) {
        AdminDocumentRequirementSearchRequest request = AdminDocumentRequirementSearchRequest.of(
                page,
                size,
                corporateType,
                documentType,
                requiredYn,
                enabledYn
        );
        return CommonResponseFactory.success(adminDocumentRequirementService.search(request));
    }

    /**
     * 필수서류 정책을 등록합니다.
     *
     * @param request 필수서류 정책 등록 요청
     * @return 등록된 필수서류 정책 응답
     */
    @Operation(summary = "필수서류 정책 등록", description = "CORPORATE_TYPE, DOCUMENT_TYPE 공통코드를 검증하고 중복 정책을 확인한 뒤 필수서류 정책을 등록합니다.")
    @ApiResponse(responseCode = "201", description = "필수서류 정책 등록 성공")
    @ApiResponse(responseCode = "409", description = "동일 법인 유형과 문서 유형 정책이 이미 존재하는 경우")
    @PostMapping
    public ResponseEntity<CommonResponse<AdminDocumentRequirementResponse>> create(
            @RequestBody(
                    description = "등록할 필수서류 정책 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AdminDocumentRequirementCreateRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody AdminDocumentRequirementCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponseFactory.success(adminDocumentRequirementService.create(request)));
    }

    /**
     * 필수서류 정책을 수정합니다.
     *
     * @param requirementId 필수서류 정책 ID
     * @param request 필수서류 정책 수정 요청 정보
     * @return 수정된 필수서류 정책 정보
     */
    @Operation(
            summary = "필수서류 정책 수정",
            description = "관리자가 법인 유형과 문서 유형 기준의 필수서류 정책을 수정한다."
    )
    @ApiResponse(responseCode = "200", description = "필수서류 정책 수정 성공")
    @ApiResponse(responseCode = "400", description = "요청값 또는 공통코드가 올바르지 않은 경우")
    @ApiResponse(responseCode = "404", description = "필수서류 정책을 찾을 수 없는 경우")
    @ApiResponse(responseCode = "409", description = "동일한 법인 유형과 문서 유형 정책이 이미 존재하는 경우")
    @PatchMapping("/{requirementId}")
    public CommonResponse<AdminDocumentRequirementResponse> update(
            @Parameter(description = "필수서류 정책 ID", required = true, example = "1")
            @PathVariable Long requirementId,
            @RequestBody(
                    description = "필수서류 정책 수정 요청 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AdminDocumentRequirementUpdateRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody AdminDocumentRequirementUpdateRequest request
    ) {
        return CommonResponseFactory.success(adminDocumentRequirementService.update(requirementId, request));
    }
}
