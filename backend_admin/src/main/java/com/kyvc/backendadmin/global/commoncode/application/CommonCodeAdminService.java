package com.kyvc.backendadmin.global.commoncode.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.global.commoncode.domain.CommonCode;
import com.kyvc.backendadmin.global.commoncode.domain.CommonCodeGroup;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeCreateRequest;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeResponse;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeUpdateRequest;
import com.kyvc.backendadmin.global.commoncode.repository.CommonCodeRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공통코드 등록/수정/활성화/비활성화/삭제 유스케이스를 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class CommonCodeAdminService {

    private static final String YES = "Y";
    private static final String NO = "N";
    private static final String DEFAULT_METADATA_JSON = "{}";

    private final CommonCodeRepository commonCodeRepository;
    private final CommonCodeQueryService commonCodeQueryService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CommonCodeResponse create(CommonCodeCreateRequest request) {
        CommonCodeGroup codeGroup = commonCodeRepository.findGroupById(request.codeGroupId())
                .orElseThrow(() -> new ApiException(ErrorCode.COMMON_CODE_GROUP_NOT_FOUND));
        // enabledYn 검증: 요청값은 Y 또는 N만 허용한다.
        String enabledYn = normalizeEnabledYn(request.enabledYn());
        // 중복 코드 검증: 동일 공통코드 그룹 안에서 같은 code 값은 한 번만 등록할 수 있다.
        if (commonCodeRepository.existsCodeByGroupIdAndCode(request.codeGroupId(), request.code())) {
            throw new ApiException(ErrorCode.COMMON_CODE_ALREADY_EXISTS);
        }
        Long adminId = SecurityUtil.getCurrentAdminId();
        CommonCode commonCode = CommonCode.create(
                codeGroup,
                request.code(),
                request.codeName(),
                request.description(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                enabledYn,
                NO,
                normalizeMetadataJson(request.metadataJson()),
                adminId
        );
        commonCodeRepository.saveCode(commonCode);
        commonCodeRepository.saveAuditLog(AuditLog.commonCode(
                adminId,
                commonCode.getCodeId(),
                "COMMON_CODE_CREATE",
                "공통코드를 등록했습니다. codeGroup=%s, code=%s".formatted(codeGroup.getCodeGroup(), request.code())
        ));
        return commonCodeQueryService.toCodeResponse(commonCode);
    }

    @Transactional
    public CommonCodeResponse update(Long codeId, CommonCodeUpdateRequest request) {
        CommonCode commonCode = getCode(codeId);
        // systemYn 코드 보호 정책: 시스템 공통코드는 운영 중 수정할 수 없다.
        validateMutable(commonCode);
        // enabledYn 검증: 요청값은 Y 또는 N만 허용한다.
        String enabledYn = normalizeEnabledYn(request.enabledYn());
        Long adminId = SecurityUtil.getCurrentAdminId();
        commonCode.update(
                request.codeName(),
                request.description(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                enabledYn,
                normalizeMetadataJson(request.metadataJson()),
                adminId
        );
        commonCodeRepository.saveAuditLog(AuditLog.commonCode(
                adminId,
                codeId,
                "COMMON_CODE_UPDATE",
                "공통코드를 수정했습니다. code=" + commonCode.getCode()
        ));
        return commonCodeQueryService.toCodeResponse(commonCode);
    }

    @Transactional
    public CommonCodeResponse enable(Long codeId) {
        return changeEnabled(codeId, YES, "COMMON_CODE_ENABLE", "공통코드를 활성화했습니다.");
    }

    @Transactional
    public CommonCodeResponse disable(Long codeId) {
        return changeEnabled(codeId, NO, "COMMON_CODE_DISABLE", "공통코드를 비활성화했습니다.");
    }

    @Transactional
    public void delete(Long codeId) {
        CommonCode commonCode = getCode(codeId);
        // systemYn 코드 보호 정책: 시스템 공통코드는 삭제할 수 없다.
        validateMutable(commonCode);
        Long adminId = SecurityUtil.getCurrentAdminId();
        commonCodeRepository.deleteCode(commonCode);
        commonCodeRepository.saveAuditLog(AuditLog.commonCode(
                adminId,
                codeId,
                "COMMON_CODE_DELETE",
                "공통코드를 삭제했습니다. code=" + commonCode.getCode()
        ));
    }

    private CommonCodeResponse changeEnabled(Long codeId, String enabledYn, String action, String description) {
        CommonCode commonCode = getCode(codeId);
        // systemYn 코드 보호 정책: 시스템 공통코드는 활성화/비활성화 상태를 변경할 수 없다.
        validateMutable(commonCode);
        Long adminId = SecurityUtil.getCurrentAdminId();
        commonCode.changeEnabled(enabledYn, adminId);
        commonCodeRepository.saveAuditLog(AuditLog.commonCode(
                adminId,
                codeId,
                action,
                description + " code=" + commonCode.getCode()
        ));
        return commonCodeQueryService.toCodeResponse(commonCode);
    }

    private CommonCode getCode(Long codeId) {
        return commonCodeRepository.findCodeById(codeId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMON_CODE_NOT_FOUND));
    }

    private void validateMutable(CommonCode commonCode) {
        if (commonCode.isSystem()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "systemYn=Y 공통코드는 수정하거나 삭제할 수 없습니다.");
        }
    }

    private String normalizeEnabledYn(String enabledYn) {
        if (enabledYn == null || enabledYn.isBlank()) {
            return YES;
        }
        if (!YES.equals(enabledYn) && !NO.equals(enabledYn)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "enabledYn은 Y 또는 N이어야 합니다.");
        }
        return enabledYn;
    }

    private String normalizeMetadataJson(String metadataJson) {
        String resolvedMetadataJson = metadataJson == null || metadataJson.isBlank()
                ? DEFAULT_METADATA_JSON
                : metadataJson;
        try {
            objectMapper.readTree(resolvedMetadataJson);
            return resolvedMetadataJson;
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "metadataJson은 유효한 JSON 문자열이어야 합니다.");
        }
    }
}
