package com.kyvc.backend.domain.corporate.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.corporate.domain.CorporateDocument;
import com.kyvc.backend.domain.corporate.repository.CorporateDocumentRepository;
import com.kyvc.backend.domain.document.application.DocumentTypeCodeNormalizer;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorage;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorageProperties;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

// 법인 단위 문서 저장 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CorporateDocumentService {

    private static final String DOCUMENT_TYPE_GROUP = "DOCUMENT_TYPE"; // 문서 유형 공통코드 그룹

    private final CorporateDocumentRepository corporateDocumentRepository;
    private final DocumentStorage documentStorage;
    private final DocumentStorageProperties documentStorageProperties;
    private final CommonCodeProvider commonCodeProvider;

    // 법인 문서 저장
    public CorporateDocument storeCorporateDocument(
            Long corporateId, // 법인 ID
            String documentTypeCode, // 문서 유형 코드
            MultipartFile file, // 업로드 파일
            Long uploadedByUserId // 업로드 사용자 ID
    ) {
        validateCorporateId(corporateId);
        validateDocumentTypeCode(documentTypeCode);
        validateFile(file);

        String normalizedDocumentTypeCode = DocumentTypeCodeNormalizer.normalize(documentTypeCode); // 정규화된 문서 유형 코드
        commonCodeProvider.validateEnabledCode(DOCUMENT_TYPE_GROUP, normalizedDocumentTypeCode);

        DocumentStorage.StoredFile storedFile = documentStorage.storeCorporateDocument(
                corporateId,
                normalizedDocumentTypeCode,
                file
        );
        CorporateDocument corporateDocument = CorporateDocument.createUploaded(
                corporateId,
                normalizedDocumentTypeCode,
                storedFile.originalFileName(),
                storedFile.storedFilePath(),
                storedFile.contentType(),
                storedFile.fileSize(),
                storedFile.fileHash(),
                KyvcEnums.UploadActorType.USER,
                uploadedByUserId
        );

        try {
            return corporateDocumentRepository.save(corporateDocument);
        } catch (DataAccessException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_SAVE_FAILED, exception);
        }
    }

    // 법인 ID 검증
    private void validateCorporateId(
            Long corporateId // 법인 ID
    ) {
        if (corporateId == null || corporateId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 문서 유형 코드 검증
    private void validateDocumentTypeCode(
            String documentTypeCode // 문서 유형 코드
    ) {
        if (!StringUtils.hasText(documentTypeCode)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
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

        String extension = originalFileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT); // 소문자 확장자
        if (!documentStorageProperties.isAllowedExtension(extension)) {
            throw new ApiException(ErrorCode.DOCUMENT_INVALID_EXTENSION);
        }
    }

    // MIME 유형 검증
    private void validateMimeType(
            MultipartFile file // 업로드 파일
    ) {
        String contentType = file.getContentType(); // MIME 타입
        if (StringUtils.hasText(contentType) && !documentStorageProperties.isAllowedMimeType(contentType)) {
            throw new ApiException(ErrorCode.DOCUMENT_MIME_TYPE_NOT_ALLOWED);
        }
    }
}
