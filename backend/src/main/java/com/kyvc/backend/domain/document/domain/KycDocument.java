package com.kyvc.backend.domain.document.domain;

import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// KYC 제출문서 엔티티
@Entity
@Table(name = "kyc_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "kyc_id", nullable = false)
    private Long kycId;

    @Column(name = "document_type_code", nullable = false, length = 100)
    private String documentTypeCode;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "document_hash", nullable = false, length = 255)
    private String documentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status_code", nullable = false, length = 50)
    private KyvcEnums.DocumentUploadStatus uploadStatus;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    // 업로드 완료 문서 생성
    public static KycDocument createUploaded(
            Long kycId, // KYC 신청 ID
            String documentTypeCode, // 문서 유형 코드
            String fileName, // 원본 파일명
            String filePath, // 저장 경로
            String mimeType, // MIME 타입
            Long fileSize, // 파일 크기
            String documentHash // 문서 해시
    ) {
        KycDocument document = new KycDocument();
        document.kycId = kycId;
        document.documentTypeCode = documentTypeCode;
        document.fileName = fileName;
        document.filePath = filePath;
        document.mimeType = mimeType;
        document.fileSize = fileSize;
        document.documentHash = documentHash;
        document.uploadStatus = KyvcEnums.DocumentUploadStatus.UPLOADED;
        document.uploadedAt = LocalDateTime.now();
        return document;
    }

    // KYC 소속 여부
    public boolean belongsToKyc(
            Long kycId // KYC 신청 ID
    ) {
        return this.kycId != null && this.kycId.equals(kycId);
    }
}
