package com.kyvc.backend.domain.core.dto;

/**
 * Core multipart 첨부 파일 파트
 *
 * @param partName multipart 파트명
 * @param fileName 원본 파일명
 * @param contentType MIME 타입
 * @param content 파일 bytes
 */
public record CoreAttachmentPart(
        String partName, // multipart 파트명
        String fileName, // 원본 파일명
        String contentType, // MIME 타입
        byte[] content // 파일 bytes
) {
}
