package com.kyvc.backend.domain.document.infrastructure;

import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
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
        return storeToDirectory(resolveTargetDirectory(kycId, documentTypeCode), file);
    }

    // 법인 단위 문서 파일 저장
    @Override
    public StoredFile storeCorporateDocument(
            Long corporateId, // 법인 ID
            String documentTypeCode, // 문서 유형 코드
            MultipartFile file // 업로드 파일
    ) {
        return storeToDirectory(resolveCorporateTargetDirectory(corporateId, documentTypeCode), file);
    }

    // 저장 문서 파일 조회
    @Override
    public StoredContent load(
            String storedFilePath // 저장 파일 경로
    ) {
        Path storedPath = resolveStoredFilePath(storedFilePath);
        if (!Files.isRegularFile(storedPath) || !Files.isReadable(storedPath)) {
            throw new ApiException(ErrorCode.DOCUMENT_FILE_NOT_FOUND);
        }

        try {
            Resource resource = new PathResource(storedPath);
            return new StoredContent(resource, Files.size(storedPath));
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_FILE_NOT_FOUND, exception);
        }
    }

    // 지정 디렉터리에 파일 저장
    private StoredFile storeToDirectory(
            Path targetDirectory, // 저장 디렉터리
            MultipartFile file // 업로드 파일
    ) {
        validateFile(file);

        String originalFileName = safeOriginalFileName(file); // 원본 파일명
        String extension = extractExtension(originalFileName); // 파일 확장자
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
                    properties.getRootPath().relativize(targetPath).toString(),
                    resolveContentType(file),
                    file.getSize(),
                    HexFormat.of().formatHex(messageDigest.digest())
            );
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_SAVE_FAILED, exception);
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

    // 법인 문서 저장 디렉터리 결정
    private Path resolveCorporateTargetDirectory(
            Long corporateId, // 법인 ID
            String documentTypeCode // 문서 유형 코드
    ) {
        Path targetDirectory = properties.getRootPath()
                .resolve("corporates")
                .resolve(String.valueOf(corporateId))
                .resolve(documentTypeCode)
                .normalize();
        if (!targetDirectory.startsWith(properties.getRootPath())) {
            throw new ApiException(ErrorCode.DOCUMENT_STORAGE_PATH_INVALID);
        }
        return targetDirectory;
    }

    // 저장 파일 경로 검증
    private Path resolveStoredFilePath(
            String storedFilePath // 저장 파일 경로
    ) {
        if (storedFilePath == null || storedFilePath.isBlank()) {
            throw new ApiException(ErrorCode.DOCUMENT_STORAGE_PATH_INVALID);
        }

        try {
            String rootRelativePath = extractStorageRootRelativePath(storedFilePath);
            if (rootRelativePath != null) {
                return resolveRelativeStoredFilePath(Path.of(rootRelativePath));
            }

            Path storedPath = Path.of(storedFilePath).normalize();
            if (!storedPath.isAbsolute()) {
                return resolveRelativeStoredFilePath(storedPath);
            }

            Path normalizedStoredPath = storedPath.toAbsolutePath().normalize();
            if (normalizedStoredPath.startsWith(properties.getRootPath())) {
                return normalizedStoredPath;
            }

            return resolveLegacyAbsoluteStoredFilePath(storedFilePath);
        } catch (InvalidPathException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_STORAGE_PATH_INVALID, exception);
        }
    }

    // 저장 루트 기준 경로 추출
    private String extractStorageRootRelativePath(
            String storedFilePath // 저장 파일 경로
    ) {
        String normalizedPath = storedFilePath.replace('\\', '/'); // 비교용 경로
        String rootName = properties.getRootPath().getFileName().toString(); // 저장 루트명
        String rootPrefix = rootName + "/";
        if (normalizedPath.startsWith(rootPrefix)) {
            return normalizedPath.substring(rootPrefix.length());
        }

        String marker = "/" + rootName + "/";
        int markerIndex = normalizedPath.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        return normalizedPath.substring(markerIndex + marker.length());
    }

    // 상대 저장 파일 경로 검증
    private Path resolveRelativeStoredFilePath(
            Path storedPath // 상대 저장 파일 경로
    ) {
        Path normalizedStoredPath = stripStorageRootName(storedPath);
        Path resolvedPath = properties.getRootPath().resolve(normalizedStoredPath).normalize();
        if (!resolvedPath.startsWith(properties.getRootPath())) {
            throw new ApiException(ErrorCode.DOCUMENT_STORAGE_PATH_INVALID);
        }
        return resolvedPath;
    }

    // 과거 절대 저장 파일 경로 보정
    private Path resolveLegacyAbsoluteStoredFilePath(
            String storedFilePath // 과거 절대 저장 파일 경로
    ) {
        String normalizedPath = storedFilePath.replace('\\', '/'); // 비교용 경로
        String rootName = properties.getRootPath().getFileName().toString(); // 저장 루트명
        String marker = "/" + rootName + "/";
        int markerIndex = normalizedPath.indexOf(marker);
        if (markerIndex < 0) {
            throw new ApiException(ErrorCode.DOCUMENT_STORAGE_PATH_INVALID);
        }

        String relativePath = normalizedPath.substring(markerIndex + marker.length());
        return resolveRelativeStoredFilePath(Path.of(relativePath));
    }

    // 저장 루트명 제거
    private Path stripStorageRootName(
            Path storedPath // 저장 파일 경로
    ) {
        if (storedPath.getNameCount() > 1
                && properties.getRootPath().getFileName().equals(storedPath.getName(0))) {
            return storedPath.subpath(1, storedPath.getNameCount());
        }
        return storedPath;
    }

    // 파일 검증
    private void validateFile(
            MultipartFile file // 업로드 파일
    ) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.DOCUMENT_FILE_REQUIRED);
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
