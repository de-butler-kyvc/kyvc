package com.kyvc.backendadmin.domain.corporate.controller;

import com.kyvc.backendadmin.domain.corporate.application.AdminCorporateService;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateAgentResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateDetailResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateDocumentResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateRepresentativeResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserDetailResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserListResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserSearchRequest;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserStatusUpdateRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 법인 사용자 및 법인 상세 관리 API를 담당합니다.
 *
 * <p>백엔드 관리자가 법인 사용자 목록/상세를 조회하고 사용자 상태를 변경하며,
 * corporateId 기준 법인 상세 정보를 조회할 수 있는 엔드포인트를 제공합니다.</p>
 */
@Tag(name = "Corporate Admin", description = "백엔드 관리자 법인 사용자 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend")
public class AdminCorporateController {

    private final AdminCorporateService adminCorporateService;

    /**
     * 법인 사용자 목록을 검색합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param keyword 사용자 이메일, 법인명, 사업자등록번호 검색어
     * @param status 사용자 상태
     * @param corporateName 법인명 검색어
     * @param kycStatus 최근 KYC 상태
     * @return 법인 사용자 목록 응답
     */
    @Operation(summary = "법인 사용자 목록 조회", description = "users, corporates, kyc_applications를 조인하여 법인 사용자 목록을 조회합니다.")
    @GetMapping("/users")
    public CommonResponse<AdminCorporateUserListResponse> searchUsers(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "사용자 이메일, 법인명, 사업자등록번호 검색어", example = "kyvc")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "사용자 상태", example = "ACTIVE")
            @RequestParam(required = false) String status,
            @Parameter(description = "법인명 검색어", example = "케이와이브이씨")
            @RequestParam(required = false) String corporateName,
            @Parameter(description = "최근 KYC 상태", example = "APPROVED")
            @RequestParam(required = false) String kycStatus
    ) {
        AdminCorporateUserSearchRequest request = AdminCorporateUserSearchRequest.of(
                page,
                size,
                keyword,
                status,
                corporateName,
                kycStatus
        );
        return CommonResponseFactory.success(adminCorporateService.searchUsers(request));
    }

    /**
     * 법인 사용자 상세 정보를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 법인 사용자 상세 응답
     */
    @Operation(summary = "법인 사용자 상세 조회", description = "사용자 정보, 법인 정보, 최근 KYC 정보를 함께 조회합니다.")
    @GetMapping("/users/{userId}")
    public CommonResponse<AdminCorporateUserDetailResponse> getUserDetail(
            @Parameter(description = "조회할 사용자 ID", example = "1")
            @PathVariable Long userId
    ) {
        return CommonResponseFactory.success(adminCorporateService.getUserDetail(userId));
    }

    /**
     * 법인 사용자 상태를 변경합니다.
     *
     * @param userId 상태를 변경할 사용자 ID
     * @param request 상태 변경 요청
     * @return 변경 후 법인 사용자 상세 응답
     */
    @Operation(summary = "법인 사용자 상태 변경", description = "사용자 상태 enum을 검증한 뒤 users.status를 변경하고 감사로그를 기록합니다.")
    @PatchMapping("/users/{userId}/status")
    public CommonResponse<AdminCorporateUserDetailResponse> updateUserStatus(
            @Parameter(description = "상태를 변경할 사용자 ID", example = "1")
            @PathVariable Long userId,
            @RequestBody(
                    description = "변경할 사용자 상태와 변경 사유",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AdminCorporateUserStatusUpdateRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody AdminCorporateUserStatusUpdateRequest request
    ) {
        return CommonResponseFactory.success(adminCorporateService.updateUserStatus(userId, request));
    }

    /**
     * 법인 상세 정보를 조회합니다.
     *
     * @param corporateId 조회할 법인 ID
     * @return 법인 상세 응답
     */
    @Operation(summary = "법인 상세 조회", description = "corporateId 기준으로 법인 기본정보와 최근 KYC 정보를 조회합니다.")
    @GetMapping("/corporates/{corporateId}")
    public CommonResponse<AdminCorporateDetailResponse> getCorporateDetail(
            @Parameter(description = "조회할 법인 ID", example = "10")
            @PathVariable Long corporateId
    ) {
        return CommonResponseFactory.success(adminCorporateService.getCorporateDetail(corporateId));
    }

    /**
     * 법인 대표자 목록을 조회합니다.
     *
     * @param corporateId 법인 ID
     * @return 법인 대표자 목록
     */
    @Operation(
            summary = "법인 대표자 목록 조회",
            description = "법인 ID 기준으로 등록된 대표자 정보를 조회합니다. 신분증 문서가 연결된 경우 문서명을 함께 반환합니다."
    )
    @ApiResponse(responseCode = "404", description = "법인을 찾을 수 없는 경우")
    @GetMapping("/corporates/{corporateId}/representatives")
    public CommonResponse<List<AdminCorporateRepresentativeResponse>> getCorporateRepresentatives(
            @Parameter(description = "법인 ID", required = true, example = "10")
            @PathVariable Long corporateId
    ) {
        return CommonResponseFactory.success(adminCorporateService.getCorporateRepresentatives(corporateId));
    }

    /**
     * 법인 대리인 목록을 조회합니다.
     *
     * @param corporateId 법인 ID
     * @return 법인 대리인 목록
     */
    @Operation(
            summary = "법인 대리인 목록 조회",
            description = "법인 ID 기준으로 등록된 대리인 정보를 조회합니다. 신분증과 위임장 문서가 연결된 경우 각각의 문서명을 함께 반환합니다."
    )
    @ApiResponse(responseCode = "404", description = "법인을 찾을 수 없는 경우")
    @GetMapping("/corporates/{corporateId}/agents")
    public CommonResponse<List<AdminCorporateAgentResponse>> getCorporateAgents(
            @Parameter(description = "법인 ID", required = true, example = "10")
            @PathVariable Long corporateId
    ) {
        return CommonResponseFactory.success(adminCorporateService.getCorporateAgents(corporateId));
    }

    /**
     * 법인문서 목록을 조회합니다.
     *
     * @param corporateId 법인 ID
     * @return 법인문서 목록
     */
    @Operation(
            summary = "법인문서 목록 조회",
            description = "법인 ID 기준으로 등록된 법인문서 메타데이터를 조회합니다. 문서 유형명과 업로드 사용자명을 함께 반환합니다."
    )
    @ApiResponse(responseCode = "404", description = "법인을 찾을 수 없는 경우")
    @GetMapping("/corporates/{corporateId}/documents")
    public CommonResponse<List<AdminCorporateDocumentResponse>> getCorporateDocuments(
            @Parameter(description = "법인 ID", required = true, example = "10")
            @PathVariable Long corporateId
    ) {
        return CommonResponseFactory.success(adminCorporateService.getCorporateDocuments(corporateId));
    }
}
