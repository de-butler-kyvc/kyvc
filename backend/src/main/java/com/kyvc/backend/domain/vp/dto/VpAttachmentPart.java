package com.kyvc.backend.domain.vp.dto;

/**
 * VP 제출 첨부 파일 파트
 *
 * @param partName multipart 파트명
 * @param fileName 원본 파일명
 * @param contentType MIME 타입
 * @param fileSize 파일 크기
 * @param content 파일 bytes
 */
public record VpAttachmentPart(
        String partName, // multipart 파트명
        String fileName, // 원본 파일명
        String contentType, // MIME 타입
        long fileSize, // 파일 크기
        byte[] content // 파일 bytes
) {
}
