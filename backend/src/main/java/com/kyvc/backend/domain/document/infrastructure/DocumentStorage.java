package com.kyvc.backend.domain.document.infrastructure;

import org.springframework.web.multipart.MultipartFile;

/**
 * 문서 파일 저장소
 */
public interface DocumentStorage {

    /**
     * 문서 파일 저장
     *
     * @param kycId KYC 신청 ID
     * @param documentTypeCode 문서 유형 코드
     * @param file 업로드 파일
     * @return 저장 파일 정보
     */
    StoredFile store(
            Long kycId, // KYC 신청 ID
            String documentTypeCode, // 문서 유형 코드
            MultipartFile file // 업로드 파일
    );

    /**
     * 법인 단위 문서 파일 저장
     *
     * @param corporateId 법인 ID
     * @param documentTypeCode 문서 유형 코드
     * @param file 업로드 파일
     * @return 저장 파일 정보
     */
    StoredFile storeCorporateDocument(
            Long corporateId, // 법인 ID
            String documentTypeCode, // 문서 유형 코드
            MultipartFile file // 업로드 파일
    );

    /**
     * 저장 파일 정보
     *
     * @param originalFileName 원본 파일명
     * @param storedFilePath 저장 파일 경로
     * @param contentType MIME 타입
     * @param fileSize 파일 크기
     * @param fileHash SHA-256 해시
     */
    record StoredFile(
            String originalFileName, // 원본 파일명
            String storedFilePath, // 저장 파일 경로
            String contentType, // MIME 타입
            Long fileSize, // 파일 크기
            String fileHash // SHA-256 해시
    ) {
    }
}
