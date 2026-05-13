package com.kyvc.backendadmin.domain.admin.controller;

import com.kyvc.backendadmin.domain.admin.application.AdminUserService;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserCreateRequest;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserDetailResponse;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserSearchRequest;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserSummaryResponse;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserUpdateRequest;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Backend Admin 관리자 계정 API 컨트롤러
/**
 * Backend Admin 관리자 계정 API를 제공하는 컨트롤러입니다.
 *
 * <p>관리자 계정 목록 조회, 상세 조회, 생성, 수정 API를 담당합니다.</p>
 */
@Tag(name = "Admin Users", description = "Backend Admin 관리자 계정 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/admin-users")
public class AdminUserController {

    // 관리자 계정 관리 서비스
    private final AdminUserService adminUserService;

    /**
     * 관리자 계정 목록을 검색한다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param keyword 이메일 또는 이름 검색어
     * @param status 관리자 상태
     * @param roleCode 권한 코드
     * @return 관리자 계정 목록
     */
    @Operation(summary = "관리자 계정 목록 조회", description = "검색 조건과 페이징으로 관리자 계정 목록을 조회합니다.")
    @GetMapping
    public CommonResponse<AdminUserSummaryResponse> search(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleCode
    ) {
        AdminUserSearchRequest request = AdminUserSearchRequest.of(page, size, keyword, status, roleCode);
        return CommonResponseFactory.success(adminUserService.search(request));
    }

    /**
     * 관리자 계정 상세 정보를 조회한다.
     *
     * @param adminUserId 관리자 ID
     * @return 관리자 계정 상세 정보
     */
    @Operation(summary = "관리자 계정 상세 조회", description = "관리자 ID로 관리자 계정 상세와 권한 목록을 조회합니다.")
    @GetMapping("/{adminUserId}")
    public CommonResponse<AdminUserDetailResponse> getDetail(@PathVariable Long adminUserId) {
        return CommonResponseFactory.success(adminUserService.getDetail(adminUserId));
    }

    /**
     * 관리자 계정을 생성한다.
     *
     * @param request 관리자 계정 생성 요청
     * @return 생성된 관리자 계정 상세 정보
     */
    @Operation(summary = "관리자 계정 생성", description = "관리자 계정을 생성하고 권한을 할당한 뒤 audit_logs에 기록합니다.")
    @PostMapping
    public CommonResponse<AdminUserDetailResponse> create(@Valid @RequestBody AdminUserCreateRequest request) {
        return CommonResponseFactory.success(adminUserService.create(request));
    }

    /**
     * 관리자 계정 정보를 수정한다.
     *
     * @param adminUserId 관리자 ID
     * @param request 관리자 계정 수정 요청
     * @return 수정된 관리자 계정 상세 정보
     */
    @Operation(summary = "관리자 계정 수정", description = "관리자 이름과 상태를 수정하고 audit_logs에 기록합니다.")
    @PatchMapping("/{adminUserId}")
    public CommonResponse<AdminUserDetailResponse> update(
            @PathVariable Long adminUserId,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return CommonResponseFactory.success(adminUserService.update(adminUserId, request));
    }
}
