package com.kyvc.backendadmin.domain.kyc.repository;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.kyc.domain.DocumentRequirement;

import java.util.Optional;

/**
 * 필수서류 정책 단건/저장 책임을 가지는 Repository입니다.
 *
 * <p>document_requirements 테이블에 정책을 저장하고, corporateType + documentType
 * 조합의 중복 정책 존재 여부와 감사로그 저장을 담당합니다.</p>
 */
public interface DocumentRequirementRepository {

    /**
     * document_requirements 테이블에 필수서류 정책을 저장합니다.
     *
     * @param documentRequirement 저장할 필수서류 정책
     * @return 저장된 필수서류 정책
     */
    DocumentRequirement save(DocumentRequirement documentRequirement);

    /**
     * document_requirements 테이블에서 필수서류 정책 ID로 정책을 조회합니다.
     *
     * @param requirementId 필수서류 정책 ID
     * @return 필수서류 정책 Optional
     */
    Optional<DocumentRequirement> findById(Long requirementId);

    /**
     * document_requirements에서 corporate_type_code와 document_type_code 조합의 중복 정책을 확인합니다.
     *
     * @param corporateType 법인 유형 공통코드
     * @param documentType 문서 유형 공통코드
     * @return 동일 정책이 있으면 true
     */
    boolean existsByCorporateTypeAndDocumentType(String corporateType, String documentType);

    /**
     * 현재 정책 ID를 제외하고 동일한 법인 유형과 문서 유형 조합이 있는지 확인합니다.
     *
     * @param corporateType 법인 유형 공통코드
     * @param documentType 문서 유형 공통코드
     * @param excludedRequirementId 제외할 필수서류 정책 ID
     * @return 다른 정책에 동일 조합이 있으면 true
     */
    boolean existsByCorporateTypeAndDocumentTypeExceptId(
            String corporateType,
            String documentType,
            Long excludedRequirementId
    );

    /**
     * audit_logs 테이블에 필수서류 정책 등록 감사로그를 저장합니다.
     *
     * @param auditLog 저장할 감사로그
     * @return 저장된 감사로그
     */
    AuditLog saveAuditLog(AuditLog auditLog);
}
