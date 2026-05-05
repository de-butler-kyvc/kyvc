package com.kyvc.backend.domain.audit.application;

import com.kyvc.backend.domain.audit.domain.AuditLog;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.audit.repository.AuditLogRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 감사로그 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // 감사로그 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(
            AuditLogCreateCommand command // 감사로그 저장 명령
    ) {
        if (command == null) {
            throw new ApiException(ErrorCode.AUDIT_LOG_SAVE_FAILED);
        }

        auditLogRepository.save(AuditLog.create(command));
    }

    // 감사로그 안전 저장
    public void saveSafely(
            AuditLogCreateCommand command // 감사로그 저장 명령
    ) {
        if (command == null) {
            return;
        }

        try {
            save(command);
        } catch (Exception exception) {
            log.warn(
                    "감사로그 저장 실패 - actionType={}, actorId={}, targetType={}, targetId={}, message={}",
                    command.actionType(),
                    command.actorId(),
                    command.auditTargetType(),
                    command.targetId(),
                    exception.getMessage()
            );
        }
    }
}
