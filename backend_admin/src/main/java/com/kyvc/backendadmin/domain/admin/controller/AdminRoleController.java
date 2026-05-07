package com.kyvc.backendadmin.domain.admin.controller;

import com.kyvc.backendadmin.domain.admin.application.AdminRoleService;
import com.kyvc.backendadmin.domain.admin.dto.AdminRoleAssignRequest;
import com.kyvc.backendadmin.domain.admin.dto.AdminRoleResponse;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 권한 관리 API를 제공하는 컨트롤러입니다.
 *
 * <p>백엔드 관리자 권한 목록 조회, 특정 관리자에게 권한 부여,
 * 특정 관리자에게 부여된 권한 회수 API를 담당합니다.</p>
 */
@Tag(name = "Admin Role", description = "백엔드 관리자 권한 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend")
public class AdminRoleController {

    // 관리자 권한 관리 서비스
    private final AdminRoleService adminRoleService;

    /**
     * 관리자 권한 목록을 조회한다.
     *
     * @return 관리자 권한 목록 응답
     */
    @Operation(
            summary = "관리자 권한 목록 조회",
            description = "admin_roles 테이블 기준으로 백엔드 관리자 권한 목록을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "권한 목록 조회 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content)
            }
    )
    @GetMapping("/admin-roles")
    public CommonResponse<List<AdminRoleResponse>> getRoles() {
        return CommonResponseFactory.success(adminRoleService.getRoles());
    }

    /**
     * 대상 관리자에게 권한을 부여한다.
     *
     * @param adminUserId 권한을 부여할 관리자 ID
     * @param request 부여할 권한 ID 요청 본문
     * @return 권한 부여 성공 응답
     */
    @Operation(
            summary = "관리자 권한 부여",
            description = "관리자 존재, 권한 존재, 권한 ACTIVE 상태, 중복 여부를 검증한 뒤 권한을 부여합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "권한 부여 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content),
                    @ApiResponse(responseCode = "404", description = "관리자 없음 또는 권한 없음", content = @Content),
                    @ApiResponse(responseCode = "409", description = "이미 부여된 권한", content = @Content)
            }
    )
    @PostMapping("/admin-users/{adminUserId}/roles")
    public CommonResponse<Void> assignRole(
            @Parameter(description = "권한을 부여할 관리자 ID", example = "1")
            @PathVariable Long adminUserId,
            @Valid @RequestBody AdminRoleAssignRequest request
    ) {
        adminRoleService.assignRole(adminUserId, request);
        return CommonResponseFactory.successWithoutData();
    }

    /**
     * 대상 관리자에게 부여된 권한을 회수한다.
     *
     * @param adminUserId 권한을 회수할 관리자 ID
     * @param roleId 회수할 권한 ID
     * @return 권한 회수 성공 응답
     */
    @Operation(
            summary = "관리자 권한 회수",
            description = "관리자 권한 매핑 존재 여부를 확인하고, 마지막 SYSTEM_ADMIN 제거와 자기 자신의 필수 권한 제거를 방지한 뒤 권한을 회수합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "권한 회수 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 부족 또는 보호 권한 회수 불가", content = @Content),
                    @ApiResponse(responseCode = "404", description = "관리자 없음, 권한 없음 또는 권한 매핑 없음", content = @Content)
            }
    )
    @DeleteMapping("/admin-users/{adminUserId}/roles/{roleId}")
    public CommonResponse<Void> revokeRole(
            @Parameter(description = "권한을 회수할 관리자 ID", example = "1")
            @PathVariable Long adminUserId,
            @Parameter(description = "회수할 권한 ID", example = "1")
            @PathVariable Long roleId
    ) {
        adminRoleService.revokeRole(adminUserId, roleId);
        return CommonResponseFactory.successWithoutData();
    }
}
