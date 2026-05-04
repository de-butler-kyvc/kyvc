package com.kyvc.backend.domain.document.repository;

import com.kyvc.backend.domain.document.domain.KycDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// KYC 문서 Repository 구현체
@Repository
@RequiredArgsConstructor
public class KycDocumentRepositoryImpl implements KycDocumentRepository {

    private final KycDocumentJpaRepository kycDocumentJpaRepository;

    // 문서 ID 기준 조회
    @Override
    public Optional<KycDocument> findById(
            Long documentId // 문서 ID
    ) {
        return kycDocumentJpaRepository.findById(documentId);
    }

    // KYC 신청 ID 기준 문서 목록 조회
    @Override
    public List<KycDocument> findByKycId(
            Long kycId // KYC 신청 ID
    ) {
        return kycDocumentJpaRepository.findByKycIdOrderByUploadedAtDesc(kycId);
    }

    // KYC 신청 ID와 문서 유형 기준 문서 목록 조회
    @Override
    public List<KycDocument> findByKycIdAndDocumentTypeCode(
            Long kycId, // KYC 신청 ID
            String documentTypeCode // 문서 유형 코드
    ) {
        return kycDocumentJpaRepository.findByKycIdAndDocumentTypeCodeOrderByUploadedAtDesc(kycId, documentTypeCode);
    }

    // KYC 신청 ID와 문서 유형 기준 문서 존재 여부
    @Override
    public boolean existsByKycIdAndDocumentTypeCode(
            Long kycId, // KYC 신청 ID
            String documentTypeCode // 문서 유형 코드
    ) {
        return kycDocumentJpaRepository.existsByKycIdAndDocumentTypeCode(kycId, documentTypeCode);
    }

    // KYC 문서 저장
    @Override
    public KycDocument save(
            KycDocument kycDocument // 저장 대상 KYC 문서
    ) {
        return kycDocumentJpaRepository.save(kycDocument);
    }
}
