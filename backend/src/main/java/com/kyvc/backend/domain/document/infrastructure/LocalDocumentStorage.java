package com.kyvc.backend.domain.document.infrastructure;

import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

// 로컬 문서 파일 저장소
@Component
public class LocalDocumentStorage implements DocumentStorage {

    private final DocumentStorageProperties properties;

    public LocalDocumentStorage(
            DocumentStorageProperties properties // 문서 저장 설정
    ) {
        this.properties = properties;
    }

    // 문서 파일 저장
    @Override
    public StoredFile store(
            Long kycId, // KYC 신청 ID
            String documentTypeCode, // 문서 유형 코드
            MultipartFile file // 업로드 파일
    ) {
        validateFile(file);

        String originalFileName = safeOriginalFileName(file); // 원본 파일명
        String extension = extractExtension(originalFileName); // 파일 확장자
        Path targetDirectory = resolveTargetDirectory(kycId, documentTypeCode); // 저장 디렉터리
        String storedFileName = UUID.randomUUID() + "." + extension; // 저장 파일명
        Path targetPath = targetDirectory.resolve(storedFileName).normalize(); // 저장 파일 경로

        if (!targetPath.startsWith(properties.getRootPath())) {
            throw new ApiException(ErrorCode.DOCUMENT_STORAGE_PATH_INVALID);
        }

        try {
            Files.createDirectories(targetDirectory);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = file.getInputStream();
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest)) {
                Files.copy(digestInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return new StoredFile(
                    originalFileName,
                    targetPath.toString(),
                    resolveContentType(file),
                    file.getSize(),
                    HexFormat.of().formatHex(messageDigest.digest())
            );
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // 저장 디렉터리 결정
    private Path resolveTargetDirectory(
            Long kycId, // KYC 신청 ID
            String documentTypeCode // 문서 유형 코드
    ) {
        Path targetDirectory = properties.getRootPath()
                .resolve(String.valueOf(kycId))
                .resolve(documentTypeCode)
                .normalize();
        if (!targetDirectory.startsWith(properties.getRootPath())) {
            throw new ApiException(ErrorCode.DOCUMENT_STORAGE_PATH_INVALID);
        }
        return targetDirectory;
    }

    // 파일 검증
    private void validateFile(
            MultipartFile file // 업로드 파일
    ) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new ApiException(ErrorCode.DOCUMENT_SIZE_EXCEEDED);
        }
    }

    // 원본 파일명 안전 추출
    private String safeOriginalFileName(
            MultipartFile file // 업로드 파일
    ) {
        try {
            String originalFileName = file.getOriginalFilename(); // 원본 파일명
            if (originalFileName == null || originalFileName.isBlank()) {
                return "document";
            }
            return Path.of(originalFileName).getFileName().toString();
        } catch (InvalidPathException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_INVALID_EXTENSION);
        }
    }

    // 파일 확장자 추출
    private String extractExtension(
            String fileName // 원본 파일명
    ) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new ApiException(ErrorCode.DOCUMENT_INVALID_EXTENSION);
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT); // 소문자 확장자
        if (!properties.isAllowedExtension(extension)) {
            throw new ApiException(ErrorCode.DOCUMENT_INVALID_EXTENSION);
        }
        return extension;
    }

    // MIME 타입 결정
    private String resolveContentType(
            MultipartFile file // 업로드 파일
    ) {
        String contentType = file.getContentType(); // MIME 타입
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }
}
