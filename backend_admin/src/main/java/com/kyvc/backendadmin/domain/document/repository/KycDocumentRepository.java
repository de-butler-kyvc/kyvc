package com.kyvc.backendadmin.domain.document.repository;

import com.kyvc.backendadmin.domain.document.domain.KycDocument;
import com.kyvc.backendadmin.global.util.KyvcEnums;

import java.util.Optional;

/**
 * KYC 제출 문서 단건 조회 책임을 가지는 Repository입니다.
 *
 * <p>kyc_documents 테이블에서 documentId 기준 단건 조회를 담당하고,
 * KYC 신청별 목록 조회는 {@link KycDocumentQueryRepository}로 분리합니다.</p>
 */
public interface KycDocumentRepository {

    /**
     * kyc_documents 테이블에서 documentId 기준으로 문서를 조회합니다.
     *
     * @param documentId 조회할 문서 ID
     * @return KYC 제출 문서 Optional
     */
    Optional<KycDocument> findById(Long documentId);

    /**
     * KYC 문서 업로드 상태를 변경합니다.
     *
     * @param documentId 문서 ID
     * @param status 변경할 업로드 상태
     * @return 수정된 행 수
     */
    int updateUploadStatus(Long documentId, KyvcEnums.DocumentUploadStatus status);
}
