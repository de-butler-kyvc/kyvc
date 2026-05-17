package com.kyvc.backendadmin.domain.document.infrastructure;

import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminDocumentStorage {

    private static final String KYC_DOCUMENTS_ROOT_NAME = "kyc-documents";
    private static final String BACKEND_ADMIN_MODULE_NAME = "backend_admin";
    private static final String BACKEND_MODULE_NAME = "backend";
    private static final String STORAGE_ROOT_NAME = "storage";

    private final AdminDocumentStorageProperties properties;

    public StoredContent load(
            String storedFilePath // 저장 파일 경로
    ) {
        Path storedPath = resolveStoredFilePath(storedFilePath);
        if (!Files.isRegularFile(storedPath) || !Files.isReadable(storedPath)) {
            throw new ApiException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        try {
            return new StoredContent(new PathResource(storedPath), Files.size(storedPath), storedPath);
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_NOT_FOUND, exception);
        }
    }

    private Path resolveStoredFilePath(
            String storedFilePath // 저장 파일 경로
    ) {
        if (!StringUtils.hasText(storedFilePath)) {
            throw new ApiException(ErrorCode.DOCUMENT_NOT_FOUND);
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
            if (allowedRoots().stream().anyMatch(normalizedStoredPath::startsWith)) {
                return normalizedStoredPath;
            }
            throw new ApiException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        } catch (InvalidPathException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_ACCESS_DENIED, exception);
        }
    }

    private Path resolveRelativeStoredFilePath(
            Path storedPath // 상대 저장 파일 경로
    ) {
        Path normalizedStoredPath = stripKnownRootName(storedPath.normalize());
        List<Path> roots = allowedRoots();
        for (Path root : roots) {
            Path candidate = root.resolve(normalizedStoredPath).normalize();
            if (!candidate.startsWith(root)) {
                throw new ApiException(ErrorCode.DOCUMENT_ACCESS_DENIED);
            }
            if (Files.isRegularFile(candidate) && Files.isReadable(candidate)) {
                return candidate;
            }
        }

        Path fallback = roots.get(0).resolve(normalizedStoredPath).normalize();
        if (!fallback.startsWith(roots.get(0))) {
            throw new ApiException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }
        return fallback;
    }

    private String extractStorageRootRelativePath(
            String storedFilePath // 저장 파일 경로
    ) {
        String normalizedPath = storedFilePath.replace('\\', '/');
        for (Path root : allowedRoots()) {
            String rootName = root.getFileName().toString();
            String rootPrefix = rootName + "/";
            if (normalizedPath.startsWith(rootPrefix)) {
                return normalizedPath.substring(rootPrefix.length());
            }

            String marker = "/" + rootName + "/";
            int markerIndex = normalizedPath.indexOf(marker);
            if (markerIndex >= 0) {
                return normalizedPath.substring(markerIndex + marker.length());
            }
        }
        return null;
    }

    private Path stripKnownRootName(
            Path storedPath // 상대 저장 파일 경로
    ) {
        if (storedPath.getNameCount() <= 1) {
            return storedPath;
        }

        String firstName = storedPath.getName(0).toString();
        for (Path root : allowedRoots()) {
            if (root.getFileName().toString().equals(firstName)) {
                return storedPath.subpath(1, storedPath.getNameCount());
            }
        }
        return storedPath;
    }

    private List<Path> allowedRoots() {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        Path rootPath = properties.getRootPath();
        roots.add(rootPath);
        if (!KYC_DOCUMENTS_ROOT_NAME.equals(rootPath.getFileName().toString())) {
            roots.add(rootPath.resolve(KYC_DOCUMENTS_ROOT_NAME).normalize());
        }
        addLocalBackendStorageRoot(roots, rootPath);
        return List.copyOf(roots);
    }

    private void addLocalBackendStorageRoot(
            LinkedHashSet<Path> roots, // 허용 저장소 루트 목록
            Path rootPath // 관리자 문서 저장소 루트
    ) {
        Path storageRoot = KYC_DOCUMENTS_ROOT_NAME.equals(rootPath.getFileName().toString())
                ? rootPath.getParent()
                : rootPath;
        if (storageRoot == null
                || storageRoot.getFileName() == null
                || !STORAGE_ROOT_NAME.equals(storageRoot.getFileName().toString())) {
            return;
        }

        Path adminModulePath = storageRoot.getParent();
        if (adminModulePath == null
                || adminModulePath.getFileName() == null
                || !BACKEND_ADMIN_MODULE_NAME.equals(adminModulePath.getFileName().toString())) {
            return;
        }

        Path workspacePath = adminModulePath.getParent();
        if (workspacePath == null) {
            return;
        }

        roots.add(workspacePath
                .resolve(BACKEND_MODULE_NAME)
                .resolve(STORAGE_ROOT_NAME)
                .resolve(KYC_DOCUMENTS_ROOT_NAME)
                .normalize());
    }

    public record StoredContent(
            Resource resource, // 파일 리소스
            long contentLength, // 파일 크기
            Path path // 파일 경로
    ) {
    }
}
