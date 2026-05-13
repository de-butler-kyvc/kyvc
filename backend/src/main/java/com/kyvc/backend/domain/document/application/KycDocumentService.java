package com.kyvc.backend.domain.document.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.dto.DocumentDeleteResponse;
import com.kyvc.backend.domain.document.dto.DocumentPreviewResponse;
import com.kyvc.backend.domain.document.dto.KycDocumentResponse;
import com.kyvc.backend.domain.document.dto.KycDocumentUploadRequest;
import com.kyvc.backend.domain.document.infrastructure.DocumentPreviewProperties;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorage;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorageProperties;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

// KYC 문서 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class KycDocumentService {

    private static final String DOCUMENT_TYPE_GROUP = "DOCUMENT_TYPE"; // 문서 유형 공통코드 그룹

    private final KycApplicationRepository kycApplicationRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final DocumentStorage documentStorage;
    private final DocumentStorageProperties documentStorageProperties;
    private final DocumentPreviewProperties documentPreviewProperties;
    private final CommonCodeProvider commonCodeProvider;

    // KYC 문서 업로드
    public KycDocumentResponse uploadDocument(
            Long userId, // 사용자 ID
            Long kycId, // KYC 요청 ID
            KycDocumentUploadRequest request // KYC 문서 업로드 요청
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        validateUploadRequest(request);

        String documentTypeCode = normalizeRequired(request.documentTypeCode());
        MultipartFile file = request.file();
        commonCodeProvider.validateEnabledCode(DOCUMENT_TYPE_GROUP, documentTypeCode);

        KycApplication kycApplication = findOwnedKyc(userId, kycId);
        if (!kycApplication.isDocumentUploadAllowed()) {
            throw new ApiException(ErrorCode.KYC_INVALID_STATUS);
        }

        validateFile(file);
        DocumentStorage.StoredFile storedFile = documentStorage.store(kycId, documentTypeCode, file);
        KycDocument kycDocument = KycDocument.createUploaded(
                kycId,
                documentTypeCode,
                storedFile.originalFileName(),
                storedFile.storedFilePath(),
                storedFile.contentType(),
                storedFile.fileSize(),
                storedFile.fileHash(),
                KyvcEnums.UploadActorType.USER,
                userId
        );
        try {
            return toResponse(kycDocumentRepository.save(kycDocument));
        } catch (DataAccessException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_SAVE_FAILED, exception);
        }
    }

    // KYC 문서 목록 조회
    @Transactional(readOnly = true)
    public List<KycDocumentResponse> getDocuments(
            Long userId, // 사용자 ID
            Long kycId // KYC 요청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        findOwnedKyc(userId, kycId);

        return kycDocumentRepository.findByKycId(kycId).stream()
                .map(this::toResponse)
                .toList();
    }

    // KYC 문서 상세 조회
    @Transactional(readOnly = true)
    public KycDocumentResponse getDocument(
            Long userId, // 사용자 ID
            Long kycId, // KYC 요청 ID
            Long documentId // 문서 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        validateDocumentId(documentId);
        findOwnedKyc(userId, kycId);

        return toResponse(findOwnedDocument(kycId, documentId));
    }

    // 문서 미리보기 URL 조회
    @Transactional(readOnly = true)
    public DocumentPreviewResponse getDocumentPreview(
            Long userId, // 사용자 ID
            Long kycId, // KYC 요청 ID
            Long documentId // 문서 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        validateDocumentId(documentId);
        findOwnedKyc(userId, kycId);

        KycDocument kycDocument = findOwnedDocument(kycId, documentId);
        if (!kycDocument.isPreviewAvailable()) {
            throw new ApiException(ErrorCode.DOCUMENT_PREVIEW_NOT_AVAILABLE);
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(documentPreviewProperties.resolvedExpirationMinutes());
        String previewUrl = UriComponentsBuilder.newInstance()
                .path("/api/user/kyc/applications/{kycId}/documents/{documentId}/preview-content")
                .queryParam("expiresAt", expiresAt)
                .buildAndExpand(kycId, documentId)
                .toUriString();

        return new DocumentPreviewResponse(previewUrl, expiresAt);
    }

    // 문서 미리보기 파일 조회
    @Transactional(readOnly = true)
    public DocumentPreviewContent getDocumentPreviewContent(
            Long userId, // 사용자 ID
            Long kycId, // KYC 신청 ID
            Long documentId, // 문서 ID
            LocalDateTime expiresAt // URL 만료 일시
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        validateDocumentId(documentId);
        validatePreviewExpiresAt(expiresAt);
        findOwnedKyc(userId, kycId);

        KycDocument kycDocument = findOwnedDocument(kycId, documentId);
        if (!kycDocument.isPreviewAvailable()) {
            throw new ApiException(ErrorCode.DOCUMENT_PREVIEW_NOT_AVAILABLE);
        }

        DocumentStorage.StoredContent storedContent = documentStorage.load(kycDocument.getFilePath());
        return new DocumentPreviewContent(
                storedContent.resource(),
                kycDocument.getFileName(),
                kycDocument.getMimeType(),
                storedContent.contentLength()
        );
    }

    // 제출 전 문서 삭제 처리
    public DocumentDeleteResponse deleteDocument(
            Long userId, // 사용자 ID
            Long kycId, // KYC 요청 ID
            Long documentId // 문서 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        validateDocumentId(documentId);

        KycApplication kycApplication = findOwnedKyc(userId, kycId);
        if (!kycApplication.isDraft()) {
            throw new ApiException(ErrorCode.KYC_INVALID_STATUS);
        }

        KycDocument kycDocument = findOwnedDocument(kycId, documentId);
        if (kycDocument.isDeleted()) {
            return new DocumentDeleteResponse(true);
        }

        kycDocument.markDeleted();
        kycDocumentRepository.save(kycDocument);
        return new DocumentDeleteResponse(true);
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

    // 사용자 소유 문서 조회
    private KycDocument findOwnedDocument(
            Long kycId, // KYC 요청 ID
            Long documentId // 문서 ID
    ) {
        KycDocument kycDocument = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
        if (!kycDocument.belongsToKyc(kycId)) {
            throw new ApiException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }
        return kycDocument;
    }

    // 업로드 요청 검증
    private void validateUploadRequest(
            KycDocumentUploadRequest request // KYC 문서 업로드 요청
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
        String originalFileName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFileName)) {
            throw new ApiException(ErrorCode.DOCUMENT_INVALID_EXTENSION);
        }

        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFileName.length() - 1) {
            throw new ApiException(ErrorCode.DOCUMENT_INVALID_EXTENSION);
        }

        String extension = originalFileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!documentStorageProperties.isAllowedExtension(extension)) {
            throw new ApiException(ErrorCode.DOCUMENT_INVALID_EXTENSION);
        }
    }

    // MIME 유형 검증
    private void validateMimeType(
            MultipartFile file // 업로드 파일
    ) {
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !documentStorageProperties.isAllowedMimeType(contentType)) {
            throw new ApiException(ErrorCode.DOCUMENT_MIME_TYPE_NOT_ALLOWED);
        }
    }

    // KYC 문서 응답 변환
    private KycDocumentResponse toResponse(
            KycDocument kycDocument // KYC 문서
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

    // 필수 문자열 정규화
    private String normalizeRequired(
            String value // 원본 문자열
    ) {
        return value.trim();
    }

    // 미리보기 만료 일시 검증
    private void validatePreviewExpiresAt(
            LocalDateTime expiresAt // URL 만료 일시
    ) {
        if (expiresAt == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (!expiresAt.isAfter(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.DOCUMENT_PREVIEW_EXPIRED);
        }
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

    // 문서 ID 검증
    private void validateDocumentId(
            Long documentId // 문서 ID
    ) {
        if (documentId == null || documentId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * 문서 미리보기 파일
     *
     * @param resource 파일 리소스
     * @param fileName 원본 파일명
     * @param mimeType MIME 타입
     * @param fileSize 파일 크기
     */
    public record DocumentPreviewContent(
            Resource resource, // 파일 리소스
            String fileName, // 원본 파일명
            String mimeType, // MIME 타입
            long fileSize // 파일 크기
    ) {
    }
}
