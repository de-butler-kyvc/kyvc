package com.kyvc.backendadmin.domain.document.infrastructure;

import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Component
@Getter
public class AdminDocumentStorageProperties {

    private final Path rootPath;

    public AdminDocumentStorageProperties(
            @Value("${kyvc.document.storage-path:${APP_STORAGE_PATH:./storage/kyc-documents}}") String storagePath // 문서 저장 루트 경로
    ) {
        this.rootPath = normalizeRootPath(storagePath);
    }

    private Path normalizeRootPath(
            String storagePath // 문서 저장 루트 경로
    ) {
        try {
            return Path.of(storagePath).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_ACCESS_DENIED, exception);
        }
    }
}
