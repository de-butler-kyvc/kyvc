package com.kyvc.backendadmin.domain.kyc.repository;

import com.kyvc.backendadmin.domain.kyc.domain.DocumentRequirement;
import com.kyvc.backendadmin.domain.kyc.dto.AdminDocumentRequirementSearchRequest;

import java.util.List;

/**
 * 필수서류 정책 목록/검색 책임을 가지는 QueryRepository입니다.
 *
 * <p>document_requirements 테이블에서 page, size, corporateType, documentType,
 * requiredYn, enabledYn 조건으로 정책 목록과 전체 건수를 조회합니다.</p>
 */
public interface DocumentRequirementQueryRepository {

    /**
     * document_requirements에서 법인 유형, 문서 유형, 필수 여부, 사용 여부 조건으로 정책 목록을 조회합니다.
     *
     * @param request 필수서류 정책 검색 조건
     * @return 필수서류 정책 목록
     */
    List<DocumentRequirement> search(AdminDocumentRequirementSearchRequest request);

    /**
     * document_requirements에서 법인 유형, 문서 유형, 필수 여부, 사용 여부 조건으로 전체 건수를 조회합니다.
     *
     * @param request 필수서류 정책 검색 조건
     * @return 전체 건수
     */
    long count(AdminDocumentRequirementSearchRequest request);
}
