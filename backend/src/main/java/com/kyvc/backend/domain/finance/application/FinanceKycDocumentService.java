package com.kyvc.backend.domain.finance.application;

import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.document.application.DocumentTypeCodeNormalizer;
import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorage;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorageProperties;
import com.kyvc.backend.domain.finance.dto.FinanceKycDocumentListResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycDocumentUploadRequest;
import com.kyvc.backend.domain.finance.dto.FinanceKycDocumentUploadResponse;
import com.kyvc.backend.domain.finance.repository.FinanceKycApplicationRepository;
import com.kyvc.backend.domain.finance.repository.FinanceKycDocumentQueryRepository;
import com.kyvc.backend.domain.finance.repository.FinanceKycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.security.CustomUserDetails;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

// 금융사 방문 KYC 문서 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class FinanceKycDocumentService {

    private static final String DOCUMENT_TYPE_GROUP = "DOCUMENT_TYPE"; // 문서 유형 공통코드 그룹

    private final FinanceContextService financeContextService;
    private final FinanceKycApplicationRepository financeKycApplicationRepository;
    private final FinanceKycDocumentRepository financeKycDocumentRepository;
    private final FinanceKycDocumentQueryRepository financeKycDocumentQueryRepository;
    private final DocumentStorage documentStorage;
    private final DocumentStorageProperties documentStorageProperties;
    private final CommonCodeProvider commonCodeProvider;
    private final AuditLogService auditLogService;

    // 금융사 방문 KYC 문서 업로드
    public FinanceKycDocumentUploadResponse uploadDocument(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long kycId, // KYC 신청 ID
            FinanceKycDocumentUploadRequest request // 문서 업로드 요청
    ) {
        validateKycId(kycId);
        validateUploadRequest(request);
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        KycApplication kycApplication = findAccessibleFinanceKyc(context, kycId);
        if (!kycApplication.isDraft()) {
            throw new ApiException(ErrorCode.FINANCE_KYC_DOCUMENT_UPLOAD_NOT_ALLOWED);
        }

        String documentTypeCode = DocumentTypeCodeNormalizer.normalize(request.documentTypeCode()); // 문서 유형 코드
        MultipartFile file = request.file(); // 업로드 파일
        commonCodeProvider.validateEnabledCode(DOCUMENT_TYPE_GROUP, documentTypeCode);
        validateFile(file);

        DocumentStorage.StoredFile storedFile = documentStorage.store(kycId, documentTypeCode, file);
        KycDocument document = KycDocument.createUploaded(
                kycId,
                documentTypeCode,
                storedFile.originalFileName(),
                storedFile.storedFilePath(),
                storedFile.contentType(),
                storedFile.fileSize(),
                storedFile.fileHash(),
                KyvcEnums.UploadActorType.FINANCE,
                context.userId()
        );

        KycDocument savedDocument;
        try {
            savedDocument = financeKycDocumentRepository.save(document);
        } catch (DataAccessException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_SAVE_FAILED, exception);
        }

        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.FINANCE.name(),
                context.userId(),
                "FINANCE_KYC_DOCUMENT_UPLOAD",
                KyvcEnums.AuditTargetType.KYC_DOCUMENT.name(),
                savedDocument.getDocumentId(),
                "금융사 방문 KYC 문서 업로드",
                null
        ));
        return new FinanceKycDocumentUploadResponse(
                savedDocument.getDocumentId(),
                savedDocument.getKycId(),
                savedDocument.getDocumentTypeCode(),
                savedDocument.getUploadStatus().name()
        );
    }

    // 금융사 방문 KYC 문서 목록 조회
    @Transactional(readOnly = true)
    public FinanceKycDocumentListResponse getDocuments(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long kycId // KYC 신청 ID
    ) {
        validateKycId(kycId);
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        findAccessibleFinanceKyc(context, kycId);
        return new FinanceKycDocumentListResponse(financeKycDocumentQueryRepository.findDocuments(kycId));
    }

    // 금융사 방문 KYC 문서 미리보기 파일 조회
    @Transactional(readOnly = true)
    public DocumentPreviewContent getPreviewContent(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long kycId, // KYC 신청 ID
            Long documentId // 문서 ID
    ) {
        validateKycId(kycId);
        validateDocumentId(documentId);
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        findAccessibleFinanceKyc(context, kycId);

        KycDocument document = financeKycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(ErrorCode.FINANCE_KYC_DOCUMENT_NOT_FOUND));
        if (!document.belongsToKyc(kycId)) {
            throw new ApiException(ErrorCode.FINANCE_KYC_DOCUMENT_ACCESS_DENIED);
        }
        if (!document.isPreviewAvailable()) {
            throw new ApiException(ErrorCode.DOCUMENT_PREVIEW_NOT_AVAILABLE);
        }

        DocumentStorage.StoredContent storedContent = documentStorage.load(document.getFilePath());
        return new DocumentPreviewContent(
                storedContent.resource(),
                document.getFileName(),
                document.getMimeType(),
                storedContent.contentLength()
        );
    }

    // 접근 가능한 금융사 방문 KYC 조회
    private KycApplication findAccessibleFinanceKyc(
            FinanceContextService.FinanceContext context, // 금융사 직원 컨텍스트
            Long kycId // KYC 신청 ID
    ) {
        KycApplication kycApplication = financeKycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.FINANCE_KYC_NOT_FOUND));
        if (!kycApplication.isFinanceVisit()) {
            throw new ApiException(ErrorCode.FINANCE_KYC_ACCESS_DENIED);
        }
        if (!kycApplication.isFinanceVisitByStaff(context.userId())) {
            throw new ApiException(ErrorCode.FINANCE_KYC_ACCESS_DENIED);
        }
        return kycApplication;
    }

    // 업로드 요청 검증
    private void validateUploadRequest(
            FinanceKycDocumentUploadRequest request // 문서 업로드 요청
    ) {
        if (request == null || !StringUtils.hasText(request.documentTypeCode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (request.file() == null) {
            throw new ApiException(ErrorCode.DOCUMENT_FILE_REQUIRED);
        }
    }

    // 업로드 파일 검증
    private void validateFile(
            MultipartFile file // 업로드 파일
    ) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.DOCUMENT_FILE_REQUIRED);
        }
        if (file.getSize() > documentStorageProperties.getMaxFileSizeBytes()) {
            throw new ApiException(ErrorCode.DOCUMENT_SIZE_EXCEEDED);
        }
        validateExtension(file);
        validateMimeType(file);
    }

    // 파일 확장자 검증
    private void validateExtension(
            MultipartFile file // 업로드 파일
    ) {
        String originalFileName = file.getOriginalFilename(); // 원본 파일명
        if (!StringUtils.hasText(originalFileName)) {
            throw new ApiException(ErrorCode.DOCUMENT_INVALID_EXTENSION);
        }

        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFileName.length() - 1) {
            throw new ApiException(ErrorCode.DOCUMENT_INVALID_EXTENSION);
        }

        String extension = originalFileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT); // 파일 확장자
        if (!documentStorageProperties.isAllowedExtension(extension)) {
            throw new ApiException(ErrorCode.DOCUMENT_INVALID_EXTENSION);
        }
    }

    // MIME 유형 검증
    private void validateMimeType(
            MultipartFile file // 업로드 파일
    ) {
        String contentType = file.getContentType(); // MIME 유형
        if (StringUtils.hasText(contentType) && !documentStorageProperties.isAllowedMimeType(contentType)) {
            throw new ApiException(ErrorCode.DOCUMENT_MIME_TYPE_NOT_ALLOWED);
        }
    }

    // KYC ID 검증
    private void validateKycId(
            Long kycId // KYC 신청 ID
    ) {
        if (kycId == null || kycId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 문서 ID 검증
    private void validateDocumentId(
            Long documentId // 문서 ID
    ) {
        if (documentId == null || documentId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 필수 문자열 정규화
    private String normalizeRequired(
            String value // 원본 문자열
    ) {
        return value.trim();
    }

    /**
     * 문서 미리보기 파일
     *
     * @param resource 파일 리소스
     * @param fileName 파일명
     * @param mimeType MIME 타입
     * @param fileSize 파일 크기
     */
    public record DocumentPreviewContent(
            Resource resource, // 파일 리소스
            String fileName, // 파일명
            String mimeType, // MIME 타입
            long fileSize // 파일 크기
    ) {
    }
}
