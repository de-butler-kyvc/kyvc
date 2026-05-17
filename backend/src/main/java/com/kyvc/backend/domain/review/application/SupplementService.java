package com.kyvc.backend.domain.review.application;

import com.kyvc.backend.domain.document.application.KycDocumentService;
import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.dto.KycDocumentResponse;
import com.kyvc.backend.domain.document.dto.KycDocumentUploadRequest;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.domain.kyc.repository.KycReviewHistoryRepository;
import com.kyvc.backend.domain.review.domain.KycSupplement;
import com.kyvc.backend.domain.review.domain.KycSupplementDocument;
import com.kyvc.backend.domain.review.dto.SupplementDetailResponse;
import com.kyvc.backend.domain.review.dto.SupplementDocumentResponse;
import com.kyvc.backend.domain.review.dto.SupplementDocumentUploadRequest;
import com.kyvc.backend.domain.review.dto.SupplementListResponse;
import com.kyvc.backend.domain.review.dto.SupplementSubmitRequest;
import com.kyvc.backend.domain.review.dto.SupplementSubmitResponse;
import com.kyvc.backend.domain.review.repository.SupplementDocumentRepository;
import com.kyvc.backend.domain.review.repository.SupplementRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

// 보완요청 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class SupplementService {

    private final KycApplicationRepository kycApplicationRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final KycDocumentService kycDocumentService;
    private final SupplementRepository supplementRepository;
    private final SupplementDocumentRepository supplementDocumentRepository;
    private final KycReviewHistoryRepository kycReviewHistoryRepository;

    // 보완요청 목록 조회
    @Transactional(readOnly = true)
    public SupplementListResponse getSupplements(
            Long userId, // 사용자 ID
            Long kycId // KYC 요청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        findOwnedKyc(userId, kycId);

        List<SupplementDetailResponse> supplements = supplementRepository.findByKycId(kycId).stream()
                .map(this::toSupplementDetailResponse)
                .toList();

        return new SupplementListResponse(supplements);
    }

    // 보완요청 상세 조회
    @Transactional(readOnly = true)
    public SupplementDetailResponse getSupplement(
            Long userId, // 사용자 ID
            Long kycId, // KYC 요청 ID
            Long supplementId // 보완요청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        validateSupplementId(supplementId);
        findOwnedKyc(userId, kycId);

        return toSupplementDetailResponse(findSupplementOfKyc(kycId, supplementId));
    }

    // 보완 문서 업로드
    public SupplementDocumentResponse uploadSupplementDocument(
            Long userId, // 사용자 ID
            Long kycId, // KYC 요청 ID
            Long supplementId, // 보완요청 ID
            SupplementDocumentUploadRequest request // 보완 문서 업로드 요청
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        validateSupplementId(supplementId);
        if (request == null || !StringUtils.hasText(request.documentTypeCode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String documentTypeCode = request.documentTypeCode().trim(); // 정규화 문서 유형 코드
        KycApplication kycApplication = findOwnedKyc(userId, kycId);
        validateKycNeedSupplement(kycApplication);

        KycSupplement supplement = findSupplementOfKyc(kycId, supplementId);
        validateSupplementRequested(supplement);
        validateRequestedDocumentType(supplement, documentTypeCode);

        KycDocumentResponse uploadedDocument = kycDocumentService.uploadDocument(
                userId,
                kycId,
                new KycDocumentUploadRequest(documentTypeCode, request.file())
        );

        KycSupplementDocument supplementDocument = supplementDocumentRepository.save(
                KycSupplementDocument.create(supplementId, uploadedDocument.documentId())
        );

        return toSupplementDocumentResponse(supplementDocument, uploadedDocument);
    }

    // 보완 제출
    public SupplementSubmitResponse submitSupplement(
            Long userId, // 사용자 ID
            Long kycId, // KYC 요청 ID
            Long supplementId, // 보완요청 ID
            SupplementSubmitRequest request // 보완 제출 요청
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        validateSupplementId(supplementId);

        KycApplication kycApplication = findOwnedKyc(userId, kycId);
        validateKycNeedSupplement(kycApplication);

        KycSupplement supplement = findSupplementOfKyc(kycId, supplementId);
        validateSupplementRequested(supplement);

        List<String> requestedDocumentTypeCodes = parseRequestedDocumentTypeCodes(supplement.getRequestedDocumentTypeCodes());
        for (String documentTypeCode : requestedDocumentTypeCodes) {
            if (!kycDocumentRepository.existsByKycIdAndDocumentTypeCode(kycId, documentTypeCode)) {
                throw new ApiException(ErrorCode.SUPPLEMENT_REQUIRED_DOCUMENT_MISSING);
            }
        }

        LocalDateTime submittedAt = LocalDateTime.now(); // 제출 일시
        KyvcEnums.KycStatus beforeKycStatus = kycApplication.getKycStatus(); // 보완 제출 전 KYC 상태
        supplement.submit(request == null ? null : request.submittedComment(), submittedAt);
        kycApplication.submitSupplement(submittedAt);

        KycSupplement savedSupplement = supplementRepository.save(supplement);
        KycApplication savedKycApplication = kycApplicationRepository.save(kycApplication);
        kycReviewHistoryRepository.saveStatusChange(
                savedKycApplication.getKycId(),
                KyvcEnums.ReviewActionType.SUPPLEMENT_SUBMIT,
                beforeKycStatus,
                savedKycApplication.getKycStatus(),
                "보완 제출 완료",
                submittedAt
        );

        return new SupplementSubmitResponse(
                savedKycApplication.getKycId(),
                savedSupplement.getSupplementId(),
                savedKycApplication.getKycStatus().name(),
                savedSupplement.getSupplementStatus().name(),
                submittedAt,
                "보완 제출이 완료되었습니다."
        );
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // KYC 요청 ID 검증
    private void validateKycId(
            Long kycId // KYC 요청 ID
    ) {
        if (kycId == null || kycId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 보완요청 ID 검증
    private void validateSupplementId(
            Long supplementId // 보완요청 ID
    ) {
        if (supplementId == null || supplementId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 사용자 소유 KYC 조회
    private KycApplication findOwnedKyc(
            Long userId, // 사용자 ID
            Long kycId // KYC 요청 ID
    ) {
        KycApplication kycApplication = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        if (!kycApplication.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.KYC_ACCESS_DENIED);
        }
        return kycApplication;
    }

    // KYC 소속 보완요청 조회
    private KycSupplement findSupplementOfKyc(
            Long kycId, // KYC 요청 ID
            Long supplementId // 보완요청 ID
    ) {
        return supplementRepository.findByIdAndKycId(supplementId, kycId)
                .orElseGet(() -> supplementRepository.findById(supplementId)
                        .map(supplement -> {
                            if (!supplement.belongsToKyc(kycId)) {
                                throw new ApiException(ErrorCode.SUPPLEMENT_ACCESS_DENIED);
                            }
                            return supplement;
                        })
                        .orElseThrow(() -> new ApiException(ErrorCode.SUPPLEMENT_NOT_FOUND)));
    }

    // 보완요청 상태 검증
    private void validateSupplementRequested(
            KycSupplement supplement // 보완요청 엔티티
    ) {
        if (KyvcEnums.SupplementStatus.SUBMITTED == supplement.getSupplementStatus()) {
            throw new ApiException(ErrorCode.SUPPLEMENT_ALREADY_SUBMITTED);
        }
        if (!supplement.isRequested()) {
            throw new ApiException(ErrorCode.SUPPLEMENT_INVALID_STATUS);
        }
    }

    // KYC 보완 필요 상태 검증
    private void validateKycNeedSupplement(
            KycApplication kycApplication // KYC 요청 엔티티
    ) {
        if (KyvcEnums.KycStatus.NEED_SUPPLEMENT != kycApplication.getKycStatus()) {
            throw new ApiException(ErrorCode.KYC_INVALID_STATUS);
        }
    }

    // 요청 문서 유형 검증
    private void validateRequestedDocumentType(
            KycSupplement supplement, // 보완요청 엔티티
            String documentTypeCode // 문서 유형 코드
    ) {
        if (!parseRequestedDocumentTypeCodes(supplement.getRequestedDocumentTypeCodes()).contains(documentTypeCode)) {
            throw new ApiException(ErrorCode.SUPPLEMENT_DOCUMENT_TYPE_NOT_ALLOWED);
        }
    }

    // 요청 문서 유형 코드 목록 파싱
    private List<String> parseRequestedDocumentTypeCodes(
            String requestedDocumentTypeCodes // 요청 문서 유형 코드 원문
    ) {
        if (!StringUtils.hasText(requestedDocumentTypeCodes)) {
            return List.of();
        }

        String normalized = requestedDocumentTypeCodes
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "");

        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    // 보완요청 상세 응답 변환
    private SupplementDetailResponse toSupplementDetailResponse(
            KycSupplement supplement // 보완요청 엔티티
    ) {
        List<SupplementDocumentResponse> uploadedDocuments = supplementDocumentRepository.findBySupplementId(
                        supplement.getSupplementId()
                ).stream()
                .map(this::toSupplementDocumentResponse)
                .toList();

        return new SupplementDetailResponse(
                supplement.getSupplementId(),
                supplement.getKycId(),
                enumName(supplement.getSupplementStatus()),
                supplement.getSupplementReasonCode(),
                supplement.getTitle(),
                supplement.getMessage(),
                supplement.getRequestReason(),
                parseRequestedDocumentTypeCodes(supplement.getRequestedDocumentTypeCodes()),
                uploadedDocuments,
                supplement.getRequestedAt(),
                supplement.getDueAt(),
                supplement.getCompletedAt(),
                supplement.getSubmittedComment()
        );
    }

    // 보완 문서 응답 변환
    private SupplementDocumentResponse toSupplementDocumentResponse(
            KycSupplementDocument supplementDocument // 보완요청 문서 매핑
    ) {
        KycDocumentResponse documentResponse = kycDocumentRepository.findById(supplementDocument.getDocumentId())
                .map(this::toKycDocumentResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));

        return toSupplementDocumentResponse(supplementDocument, documentResponse);
    }

    // 보완 문서 응답 변환
    private SupplementDocumentResponse toSupplementDocumentResponse(
            KycSupplementDocument supplementDocument, // 보완요청 문서 매핑
            KycDocumentResponse documentResponse // 문서 응답 정보
    ) {
        return new SupplementDocumentResponse(
                supplementDocument.getSupplementDocumentId(),
                documentResponse.documentId(),
                documentResponse.documentTypeCode(),
                documentResponse.fileName(),
                documentResponse.mimeType(),
                documentResponse.fileSize(),
                documentResponse.documentHash(),
                documentResponse.uploadedAt()
        );
    }

    // KYC 문서 응답 변환
    private KycDocumentResponse toKycDocumentResponse(
            KycDocument kycDocument // KYC 문서 엔티티
    ) {
        return new KycDocumentResponse(
                kycDocument.getDocumentId(),
                kycDocument.getKycId(),
                kycDocument.getDocumentTypeCode(),
                kycDocument.getFileName(),
                kycDocument.getMimeType(),
                kycDocument.getFileSize(),
                kycDocument.getDocumentHash(),
                kycDocument.getUploadStatus().name(),
                kycDocument.getUploadedAt()
        );
    }

    // enum 이름 변환
    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }
}
