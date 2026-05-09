package com.kyvc.backendadmin.domain.corporate.repository;

import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateAgentResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateDocumentResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateRepresentativeResponse;

import java.util.List;

/**
 * 법인 부가정보 조회 QueryRepository입니다.
 */
public interface AdminCorporateAdditionalInfoQueryRepository {

    /**
     * 법인 ID 기준으로 대표자 목록을 조회합니다.
     *
     * @param corporateId 법인 ID
     * @return 법인 대표자 목록
     */
    List<AdminCorporateRepresentativeResponse> findRepresentativesByCorporateId(Long corporateId);

    /**
     * 법인 ID 기준으로 대리인 목록을 조회합니다.
     *
     * @param corporateId 법인 ID
     * @return 법인 대리인 목록
     */
    List<AdminCorporateAgentResponse> findAgentsByCorporateId(Long corporateId);

    /**
     * 법인 ID 기준으로 법인문서 목록을 조회합니다.
     *
     * @param corporateId 법인 ID
     * @return 법인문서 목록
     */
    List<AdminCorporateDocumentResponse> findDocumentsByCorporateId(Long corporateId);
}
