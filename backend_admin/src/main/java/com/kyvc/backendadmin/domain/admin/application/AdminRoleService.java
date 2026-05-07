package com.kyvc.backendadmin.domain.admin.application;

import com.kyvc.backendadmin.domain.admin.domain.AdminRole;
import com.kyvc.backendadmin.domain.admin.domain.AdminUser;
import com.kyvc.backendadmin.domain.admin.domain.AdminUserRole;
import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.admin.dto.AdminRoleAssignRequest;
import com.kyvc.backendadmin.domain.admin.dto.AdminRoleResponse;
import com.kyvc.backendadmin.domain.admin.repository.AdminRoleRepository;
import com.kyvc.backendadmin.domain.admin.repository.AdminUserRepository;
import com.kyvc.backendadmin.domain.admin.repository.AdminUserRoleRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 관리자 권한 관리 유스케이스를 처리하는 서비스입니다.
 *
 * <p>권한 목록 조회, 관리자 권한 부여, 관리자 권한 회수를 담당합니다.
 * 권한 부여와 회수에서는 대상 관리자 검증, 권한 검증, 중복/매핑 검증을 수행하고
 * 주요 변경 사항을 audit_logs에 기록합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminRoleService {

    // 자기 자신의 필수 권한으로 간주해 제거를 막는 권한 코드
    private static final Set<KyvcEnums.RoleCode> SELF_REQUIRED_ROLES = Set.of(
            KyvcEnums.RoleCode.SYSTEM_ADMIN,
            KyvcEnums.RoleCode.BACKEND_ADMIN
    );

    // 관리자 권한 조회 저장소
    private final AdminRoleRepository adminRoleRepository;
    // 관리자 계정 및 감사 로그 저장소
    private final AdminUserRepository adminUserRepository;
    // 관리자-권한 매핑 저장소
    private final AdminUserRoleRepository adminUserRoleRepository;

    /**
     * 관리자 권한 목록을 조회합니다.
     *
     * <p>admin_roles 테이블 기준으로 전체 권한을 조회하며, 별도의 관리자 검증이나
     * 감사 로그 기록은 수행하지 않습니다.</p>
     *
     * @return 관리자 권한 목록
     */
    @Transactional(readOnly = true)
    public List<AdminRoleResponse> getRoles() {
        return adminRoleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 대상 관리자에게 권한을 부여합니다.
     *
     * <p>대상 관리자 존재 여부, 권한 존재 여부, 권한 ACTIVE 상태, 이미 부여된 권한인지
     * 중복 여부를 검증합니다. 검증을 통과하면 admin_user_roles에 매핑을 저장하고
     * audit_logs에 권한 부여 이력을 기록합니다.</p>
     *
     * @param adminUserId 권한을 부여할 관리자 ID
     * @param request 부여할 권한 ID 요청
     */
    @Transactional
    public void assignRole(Long adminUserId, AdminRoleAssignRequest request) {
        AdminUser adminUser = getAdminUser(adminUserId);
        AdminRole adminRole = getActiveRole(request.roleId());
        // 중복 권한 부여 검증: 이미 같은 role_id가 매핑되어 있으면 409로 응답한다.
        if (adminUserRoleRepository.existsByAdminUserIdAndRoleId(adminUserId, request.roleId())) {
            throw new ApiException(ErrorCode.ADMIN_ROLE_ALREADY_ASSIGNED);
        }

        adminUserRoleRepository.save(AdminUserRole.create(adminUser, adminRole));
        writeAuditLog(adminUserId, "ADMIN_ROLE_ASSIGN", "관리자 권한을 부여했습니다: " + adminRole.getRoleCode().name());
    }

    /**
     * 대상 관리자에게 부여된 권한을 회수합니다.
     *
     * <p>대상 관리자 존재 여부, 권한 존재 여부, 매핑 존재 여부를 검증합니다.
     * SYSTEM_ADMIN 권한은 마지막 보유자 제거를 방지하고, 현재 관리자가 자기 자신의 필수 권한을
     * 제거하지 못하도록 검증합니다. 회수 성공 시 audit_logs에 권한 회수 이력을 기록합니다.</p>
     *
     * @param adminUserId 권한을 회수할 관리자 ID
     * @param roleId 회수할 권한 ID
     */
    @Transactional
    public void revokeRole(Long adminUserId, Long roleId) {
        getAdminUser(adminUserId);
        AdminRole adminRole = getRole(roleId);
        AdminUserRole userRole = adminUserRoleRepository.findByAdminUserIdAndRoleId(adminUserId, roleId)
                .orElseThrow(() -> new ApiException(ErrorCode.ADMIN_ROLE_NOT_ASSIGNED));

        validateRoleRevocation(adminUserId, adminRole);
        adminUserRoleRepository.delete(userRole);
        writeAuditLog(adminUserId, "ADMIN_ROLE_REVOKE", "관리자 권한을 회수했습니다: " + adminRole.getRoleCode().name());
    }

    private AdminUser getAdminUser(Long adminUserId) {
        return adminUserRepository.findById(adminUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.ADMIN_NOT_FOUND));
    }

    private AdminRole getRole(Long roleId) {
        return adminRoleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException(ErrorCode.ADMIN_ROLE_NOT_FOUND));
    }

    private AdminRole getActiveRole(Long roleId) {
        AdminRole adminRole = getRole(roleId);
        if (!adminRole.isActive()) {
            throw new ApiException(ErrorCode.INVALID_STATUS);
        }
        return adminRole;
    }

    private void validateRoleRevocation(Long adminUserId, AdminRole adminRole) {
        KyvcEnums.RoleCode roleCode = adminRole.getRoleCode();
        // 마지막 SYSTEM_ADMIN 제거 방지: 시스템 관리자가 1명 이하이면 회수를 막는다.
        if (KyvcEnums.RoleCode.SYSTEM_ADMIN == roleCode
                && adminUserRoleRepository.countByRoleCode(KyvcEnums.RoleCode.SYSTEM_ADMIN) <= 1) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        // 자기 자신의 필수 권한 제거 방지: 현재 관리자의 핵심 운영 권한은 스스로 회수할 수 없다.
        if (SecurityUtil.getCurrentAdminId().equals(adminUserId) && SELF_REQUIRED_ROLES.contains(roleCode)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private void writeAuditLog(Long targetAdminId, String action, String description) {
        adminUserRepository.saveAuditLog(AuditLog.adminUser(
                SecurityUtil.getCurrentAdminId(),
                targetAdminId,
                action,
                description
        ));
    }

    private AdminRoleResponse toResponse(AdminRole adminRole) {
        return new AdminRoleResponse(
                adminRole.getRoleId(),
                adminRole.getRoleCode().name(),
                adminRole.getRoleName(),
                adminRole.getStatus()
        );
    }
}
