package com.kyvc.backendadmin.domain.verifier.controller;

import com.kyvc.backendadmin.domain.verifier.application.AdminVerifierQueryService;
import com.kyvc.backendadmin.domain.verifier.application.AdminVerifierService;
import com.kyvc.backendadmin.domain.verifier.dto.AdminVerifierDtos;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Verifier 플랫폼 관리 API를 제공합니다.
 */
@Tag(name = "Backend Admin Verifier", description = "Verifier 플랫폼 등록, 수정, 승인, 중지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/verifiers")
public class AdminVerifierController {

    private final AdminVerifierService adminVerifierService;
    private final AdminVerifierQueryService adminVerifierQueryService;

    /**
     * Verifier 목록을 조회합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status 상태 코드
     * @param keyword 검색어
     * @return Verifier 목록
     */
    @Operation(summary = "Verifier 목록 조회", description = "Verifier 플랫폼 목록을 페이지, 상태, 키워드 조건으로 조회한다.")
    @GetMapping
    public CommonResponse<AdminVerifierDtos.PageResponse> search(
            @Parameter(description = "페이지 번호", example = "0") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(required = false) Integer size,
            @Parameter(description = "상태 코드", example = "PENDING") @RequestParam(required = false) String status,
            @Parameter(description = "검색어", example = "Partner") @RequestParam(required = false) String keyword
    ) {
        return CommonResponseFactory.success(adminVerifierQueryService.search(page, size, status, keyword));
    }

    /**
     * Verifier를 등록합니다.
     *
     * @param request 등록 요청
     * @return 등록된 Verifier
     */
    @Operation(summary = "Verifier 등록", description = "Verifier 플랫폼을 PENDING 상태로 등록하고 Callback URL이 있으면 함께 저장한다.")
    @PostMapping
    public CommonResponse<AdminVerifierDtos.Response> create(@Valid @RequestBody AdminVerifierDtos.CreateRequest request) {
        return CommonResponseFactory.success(adminVerifierService.create(request));
    }

    /**
     * Verifier 상세를 조회합니다.
     *
     * @param verifierId Verifier ID
     * @return Verifier 상세
     */
    @Operation(summary = "Verifier 상세 조회", description = "Verifier 기본정보, Callback, API Key 요약, 최근 사용량을 조회한다.")
    @GetMapping("/{verifierId}")
    public CommonResponse<AdminVerifierDtos.DetailResponse> getDetail(
            @Parameter(description = "Verifier ID", required = true) @PathVariable Long verifierId
    ) {
        return CommonResponseFactory.success(adminVerifierQueryService.getDetail(verifierId));
    }

    /**
     * Verifier를 수정합니다.
     *
     * @param verifierId Verifier ID
     * @param request 수정 요청
     * @return 수정된 Verifier
     */
    @Operation(summary = "Verifier 수정", description = "Verifier 이름, 담당자 이메일, Callback URL 등 수정 가능한 필드만 변경한다.")
    @PatchMapping("/{verifierId}")
    public CommonResponse<AdminVerifierDtos.Response> update(
            @PathVariable Long verifierId,
            @Valid @RequestBody AdminVerifierDtos.UpdateRequest request
    ) {
        return CommonResponseFactory.success(adminVerifierService.update(verifierId, request));
    }

    /**
     * Verifier를 승인합니다.
     *
     * @param verifierId Verifier ID
     * @param request 승인 요청
     * @return 승인된 Verifier
     */
    @Operation(summary = "Verifier 승인", description = "PENDING 또는 REJECTED 상태의 Verifier를 MFA 검증 후 APPROVED 상태로 변경한다.")
    @PostMapping("/{verifierId}/approve")
    public CommonResponse<AdminVerifierDtos.Response> approve(
            @PathVariable Long verifierId,
            @Valid @RequestBody AdminVerifierDtos.ApproveRequest request
    ) {
        return CommonResponseFactory.success(adminVerifierService.approve(verifierId, request));
    }

    /**
     * Verifier를 중지합니다.
     *
     * @param verifierId Verifier ID
     * @param request 중지 요청
     * @return 중지된 Verifier
     */
    @Operation(summary = "Verifier 중지", description = "APPROVED 상태의 Verifier를 MFA 검증 후 SUSPENDED 상태로 변경하고 ACTIVE API Key를 폐기한다.")
    @PostMapping("/{verifierId}/suspend")
    public CommonResponse<AdminVerifierDtos.Response> suspend(
            @PathVariable Long verifierId,
            @Valid @RequestBody AdminVerifierDtos.SuspendRequest request
    ) {
        return CommonResponseFactory.success(adminVerifierService.suspend(verifierId, request));
    }
}
