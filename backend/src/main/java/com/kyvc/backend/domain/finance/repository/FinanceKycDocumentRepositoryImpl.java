package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 금융사 방문 KYC 문서 Repository 구현체
@Repository
@RequiredArgsConstructor
public class FinanceKycDocumentRepositoryImpl implements FinanceKycDocumentRepository {

    private final KycDocumentRepository kycDocumentRepository;

    // 문서 ID 기준 조회
    @Override
    public Optional<KycDocument> findById(
            Long documentId // 문서 ID
    ) {
        return kycDocumentRepository.findById(documentId);
    }

    // KYC 신청 ID 기준 목록 조회
    @Override
    public List<KycDocument> findByKycId(
            Long kycId // KYC 신청 ID
    ) {
        return kycDocumentRepository.findByKycId(kycId);
    }

    // 문서 저장
    @Override
    public KycDocument save(
            KycDocument kycDocument // 저장 대상 문서
    ) {
        return kycDocumentRepository.save(kycDocument);
    }
}
