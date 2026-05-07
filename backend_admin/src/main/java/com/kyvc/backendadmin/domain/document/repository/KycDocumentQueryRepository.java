package com.kyvc.backendadmin.domain.document.repository;

import com.kyvc.backendadmin.domain.document.dto.AdminKycDocumentListResponse;

import java.util.List;

/**
 * KYC 제출 문서 목록 조회 책임을 가지는 QueryRepository입니다.
 *
 * <p>kyc_documents를 kycId 기준으로 조회하고, DOCUMENT_TYPE 공통코드와 조인하여
 * 문서 유형명을 함께 조회합니다.</p>
 */
public interface KycDocumentQueryRepository {

    /**
     * kyc_documents.kyc_id 기준으로 제출 문서 목록을 조회합니다.
     *
     * <p>common_code_groups.code_group = DOCUMENT_TYPE 조건으로 common_codes를 조인하여
     * document_type_code에 대응하는 문서 유형명을 매핑합니다.</p>
     *
     * @param kycId 조회할 KYC 신청 ID
     * @return KYC 제출 문서 목록
     */
    List<AdminKycDocumentListResponse.Item> findDocumentsByKycId(Long kycId);

    /**
     * DOCUMENT_TYPE 공통코드에서 문서 유형 코드를 문서 유형명으로 변환합니다.
     *
     * @param documentTypeCode 문서 유형 코드
     * @return 문서 유형명 또는 null
     */
    String findDocumentTypeName(String documentTypeCode);
}
