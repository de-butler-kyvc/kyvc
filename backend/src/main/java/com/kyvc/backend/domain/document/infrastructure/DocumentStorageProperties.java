package com.kyvc.backend.domain.document.infrastructure;

import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// 문서 저장 설정
@Component
@Getter
public class DocumentStorageProperties {

    private final Path rootPath; // 문서 저장 루트 경로
    private final int maxFileSizeMb; // 최대 파일 크기 MB
    private final long maxFileSizeBytes; // 최대 파일 크기 byte
    private final List<String> allowedExtensions; // 허용 확장자 목록
    private final Set<String> allowedExtensionSet; // 허용 확장자 검증 목록
    private final Set<String> allowedMimeTypes; // 허용 MIME 타입 목록

    public DocumentStorageProperties(
            @Value("${kyvc.document.storage-path:./storage/kyc-documents}") String storagePath, // 문서 저장 루트 경로
            @Value("${kyvc.document.max-file-size-mb:10}") int maxFileSizeMb, // 최대 파일 크기 MB
            @Value("${kyvc.document.allowed-extensions:pdf,jpg,jpeg,png}") String allowedExtensions, // 허용 확장자 목록
            @Value("${kyvc.document.allowed-mime-types:application/pdf,image/jpeg,image/png}") String allowedMimeTypes // 허용 MIME 타입 목록
    ) {
        this.rootPath = normalizeRootPath(storagePath);
        this.maxFileSizeMb = maxFileSizeMb;
        this.maxFileSizeBytes = maxFileSizeMb * 1024L * 1024L;
        this.allowedExtensions = parseCsv(allowedExtensions);
        this.allowedExtensionSet = Set.copyOf(this.allowedExtensions);
        this.allowedMimeTypes = Set.copyOf(parseCsv(allowedMimeTypes));
    }

    // 허용 확장자 여부
    public boolean isAllowedExtension(
            String extension // 파일 확장자
    ) {
        return allowedExtensionSet.contains(extension.toLowerCase(Locale.ROOT));
    }

    // 허용 MIME 타입 여부
    public boolean isAllowedMimeType(
            String mimeType // MIME 타입
    ) {
        return allowedMimeTypes.contains(mimeType.toLowerCase(Locale.ROOT));
    }

    // 저장 루트 경로 정규화
    private Path normalizeRootPath(
            String storagePath // 문서 저장 루트 경로
    ) {
        try {
            return Path.of(storagePath).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new ApiException(ErrorCode.DOCUMENT_STORAGE_PATH_INVALID);
        }
    }

    // CSV 설정값 변환
    private List<String> parseCsv(
            String values // CSV 설정값
    ) {
        if (!StringUtils.hasText(values)) {
            return List.of();
        }
        return Arrays.stream(values.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}
