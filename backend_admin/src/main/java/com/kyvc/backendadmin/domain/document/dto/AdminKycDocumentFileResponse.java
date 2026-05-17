package com.kyvc.backendadmin.domain.document.dto;

import org.springframework.core.io.Resource;

public record AdminKycDocumentFileResponse(
        Resource resource, // 파일 리소스
        String fileName, // 원본 파일명
        String mimeType, // MIME 타입
        long fileSize // 파일 크기
) {
}
