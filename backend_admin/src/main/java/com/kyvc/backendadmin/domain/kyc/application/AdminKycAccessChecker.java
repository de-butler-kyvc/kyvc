package com.kyvc.backendadmin.domain.kyc.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminKycAccessChecker {

    private static final String SCOPE_NOT_CONFIGURED = "KYC_SCOPE_COLUMNS_NOT_CONFIGURED";

    private final AuditLogWriter auditLogWriter;

    public void validateActionAccess(Long kycId, String actionType) {
        Long adminId = SecurityUtil.getCurrentAdminId();
        if (SecurityUtil.hasRole(KyvcEnums.RoleCode.SYSTEM_ADMIN.name())) {
            return;
        }
        if (SecurityUtil.hasRole(KyvcEnums.RoleCode.BACKEND_ADMIN.name())) {
            writeFailureAudit(adminId, kycId, actionType, SCOPE_NOT_CONFIGURED);
            throw new ApiException(ErrorCode.FORBIDDEN, "KYC 처리 범위 컬럼이 구성되지 않았습니다.");
        }
        writeFailureAudit(adminId, kycId, actionType, "ROLE_NOT_ALLOWED");
        throw new ApiException(ErrorCode.FORBIDDEN);
    }

    private void writeFailureAudit(Long adminId, Long kycId, String actionType, String reasonCode) {
        auditLogWriter.write(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                actionType,
                KyvcEnums.AuditTargetType.KYC_APPLICATION,
                kycId,
                "KYC 처리 범위 검증 실패. reasonCode=%s".formatted(reasonCode),
                null,
                "FAILURE"
        );
    }
}
