package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.CorporateDocument;

/**
 * 법인 문서 Repository
 */
public interface CorporateDocumentRepository {

    /**
     * 법인 문서 저장
     *
     * @param corporateDocument 저장 대상 법인 문서
     * @return 저장된 법인 문서
     */
    CorporateDocument save(
            CorporateDocument corporateDocument // 저장 대상 법인 문서
    );
}
