package com.kyvc.backend.domain.audit.application;

import com.kyvc.backend.domain.audit.domain.AuditLog;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.audit.dto.InternalAuditLogRequest;
import com.kyvc.backend.domain.audit.dto.InternalAuditLogResponse;
import com.kyvc.backend.domain.audit.repository.AuditLogRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

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

    // 내부 감사로그 기록
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InternalAuditLogResponse createInternalAuditLog(
            InternalAuditLogRequest request // 내부 감사로그 기록 요청
    ) {
        validateInternalAuditLogRequest(request);

        AuditLogCreateCommand command = new AuditLogCreateCommand(
                normalizeActorType(request.actorType()),
                request.actorId(),
                request.actionType().trim(),
                normalizeAuditTargetType(request.auditTargetType()),
                request.targetId(),
                normalizeNullable(request.requestSummary()),
                normalizeNullable(request.ipAddress())
        );

        AuditLog savedAuditLog;
        try {
            savedAuditLog = auditLogRepository.save(AuditLog.create(command));
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INTERNAL_AUDIT_LOG_SAVE_FAILED);
        }

        return new InternalAuditLogResponse(savedAuditLog.getAuditLogId(), true);
    }

    // 내부 감사로그 요청 검증
    private void validateInternalAuditLogRequest(
            InternalAuditLogRequest request // 내부 감사로그 기록 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.actorType())
                || request.actorId() == null
                || request.actorId() < 0
                || !StringUtils.hasText(request.actionType())
                || !StringUtils.hasText(request.auditTargetType())
                || request.targetId() == null
                || request.targetId() <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 행위자 유형 정규화
    private String normalizeActorType(
            String actorType // 원본 행위자 유형 코드
    ) {
        String normalizedActorType = actorType.trim().toUpperCase(Locale.ROOT); // 정규화 행위자 유형 코드
        try {
            KyvcEnums.ActorType.valueOf(normalizedActorType);
            return normalizedActorType;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 감사 대상 유형 정규화
    private String normalizeAuditTargetType(
            String auditTargetType // 원본 감사 대상 유형 코드
    ) {
        String normalizedAuditTargetType = auditTargetType.trim().toUpperCase(Locale.ROOT); // 정규화 감사 대상 유형 코드
        try {
            KyvcEnums.AuditTargetType.valueOf(normalizedAuditTargetType);
            return normalizedAuditTargetType;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 선택 문자열 정규화
    private String normalizeNullable(
            String value // 원본 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

