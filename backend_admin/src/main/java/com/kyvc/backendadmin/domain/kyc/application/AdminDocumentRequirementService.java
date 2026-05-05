package com.kyvc.backendadmin.domain.kyc.application;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.kyc.domain.DocumentRequirement;
import com.kyvc.backendadmin.domain.kyc.dto.AdminDocumentRequirementCreateRequest;
import com.kyvc.backendadmin.domain.kyc.dto.AdminDocumentRequirementListResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminDocumentRequirementResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminDocumentRequirementSearchRequest;
import com.kyvc.backendadmin.domain.kyc.repository.DocumentRequirementQueryRepository;
import com.kyvc.backendadmin.domain.kyc.repository.DocumentRequirementRepository;
import com.kyvc.backendadmin.global.commoncode.application.CommonCodeValidator;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 필수서류 정책 조회/등록 유스케이스를 담당합니다.
 *
 * <p>법인 유형별 필수서류 정책 목록 조회와 신규 정책 등록을 처리하며,
 * 등록 시 공통코드 검증, 중복 정책 확인, 감사로그 기록을 수행합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminDocumentRequirementService {

    private static final String CORPORATE_TYPE_CODE_GROUP = "CORPORATE_TYPE";
    private static final String DOCUMENT_TYPE_CODE_GROUP = "DOCUMENT_TYPE";

    private final DocumentRequirementRepository documentRequirementRepository;
    private final DocumentRequirementQueryRepository documentRequirementQueryRepository;
    private final CommonCodeValidator commonCodeValidator;

    /**
     * 필수서류 정책 목록을 검색합니다.
     *
     * <p>page, size, corporateType, documentType, requiredYn, enabledYn 조건으로 조회합니다.
     * 조회 조건 중 corporateType과 documentType이 전달되면 CommonCodeValidator로 공통코드를 검증합니다.
     * 목록 조회이므로 중복 정책 확인과 감사로그 기록은 수행하지 않습니다.</p>
     *
     * @param request 필수서류 정책 검색 조건
     * @return 필수서류 정책 목록 응답
     */
    @Transactional(readOnly = true)
    public AdminDocumentRequirementListResponse search(AdminDocumentRequirementSearchRequest request) {
        validateSearchRequest(request);
        List<AdminDocumentRequirementListResponse.Item> items = documentRequirementQueryRepository.search(request)
                .stream()
                .map(this::toListItem)
                .toList();
        long totalElements = documentRequirementQueryRepository.count(request);
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / request.size());
        return new AdminDocumentRequirementListResponse(
                items,
                request.page(),
                request.size(),
                totalElements,
                totalPages
        );
    }

    /**
     * 필수서류 정책을 등록합니다.
     *
     * <p>CORPORATE_TYPE, DOCUMENT_TYPE 공통코드를 CommonCodeValidator로 검증하고,
     * 동일 corporateType + documentType 정책이 이미 있으면 DOCUMENT_REQUIREMENT_ALREADY_EXISTS를 던집니다.
     * 신규 정책 저장 후 audit_logs에 등록 감사로그를 기록합니다.</p>
     *
     * @param request 필수서류 정책 등록 요청
     * @return 등록된 필수서류 정책 응답
     */
    @Transactional
    public AdminDocumentRequirementResponse create(AdminDocumentRequirementCreateRequest request) {
        validateYn(request.requiredYn(), "requiredYn");
        validateYn(request.enabledYn(), "enabledYn");

        // CORPORATE_TYPE 공통코드 검증: 활성 법인 유형 코드만 정책 등록에 사용할 수 있다.
        commonCodeValidator.validateEnabledCode(CORPORATE_TYPE_CODE_GROUP, request.corporateType());
        // DOCUMENT_TYPE 공통코드 검증: 활성 문서 유형 코드만 정책 등록에 사용할 수 있다.
        commonCodeValidator.validateEnabledCode(DOCUMENT_TYPE_CODE_GROUP, request.documentType());

        // 중복 정책 확인: 동일 법인 유형 + 문서 유형 조합은 하나의 정책만 허용한다.
        if (documentRequirementRepository.existsByCorporateTypeAndDocumentType(
                request.corporateType(),
                request.documentType()
        )) {
            throw new ApiException(ErrorCode.DOCUMENT_REQUIREMENT_ALREADY_EXISTS);
        }

        Long adminId = SecurityUtil.getCurrentAdminId();
        DocumentRequirement documentRequirement = DocumentRequirement.create(
                request.corporateType(),
                request.documentType(),
                request.requiredYn(),
                request.enabledYn(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                request.guideMessage(),
                adminId
        );
        documentRequirementRepository.save(documentRequirement);

        documentRequirementRepository.saveAuditLog(AuditLog.documentRequirement(
                adminId,
                documentRequirement.getRequirementId(),
                "DOCUMENT_REQUIREMENT_CREATE",
                "필수서류 정책이 등록되었습니다. corporateType=%s, documentType=%s"
                        .formatted(request.corporateType(), request.documentType())
        ));

        return toResponse(documentRequirement);
    }

    private void validateSearchRequest(AdminDocumentRequirementSearchRequest request) {
        if (StringUtils.hasText(request.corporateType())) {
            commonCodeValidator.validateEnabledCode(CORPORATE_TYPE_CODE_GROUP, request.corporateType());
        }
        if (StringUtils.hasText(request.documentType())) {
            commonCodeValidator.validateEnabledCode(DOCUMENT_TYPE_CODE_GROUP, request.documentType());
        }
        if (StringUtils.hasText(request.requiredYn())) {
            validateYn(request.requiredYn(), "requiredYn");
        }
        if (StringUtils.hasText(request.enabledYn())) {
            validateYn(request.enabledYn(), "enabledYn");
        }
    }

    private void validateYn(String value, String fieldName) {
        if (!"Y".equals(value) && !"N".equals(value)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, fieldName + "은 Y 또는 N이어야 합니다.");
        }
    }

    private AdminDocumentRequirementListResponse.Item toListItem(DocumentRequirement requirement) {
        return new AdminDocumentRequirementListResponse.Item(
                requirement.getRequirementId(),
                requirement.getCorporateTypeCode(),
                requirement.getDocumentTypeCode(),
                requirement.getRequiredYn(),
                requirement.getEnabledYn(),
                requirement.getSortOrder(),
                requirement.getGuideMessage(),
                requirement.getCreatedByAdminId(),
                requirement.getUpdatedByAdminId(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt()
        );
    }

    private AdminDocumentRequirementResponse toResponse(DocumentRequirement requirement) {
        return new AdminDocumentRequirementResponse(
                requirement.getRequirementId(),
                requirement.getCorporateTypeCode(),
                requirement.getDocumentTypeCode(),
                requirement.getRequiredYn(),
                requirement.getEnabledYn(),
                requirement.getSortOrder(),
                requirement.getGuideMessage(),
                requirement.getCreatedByAdminId(),
                requirement.getUpdatedByAdminId(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt()
        );
    }
}
