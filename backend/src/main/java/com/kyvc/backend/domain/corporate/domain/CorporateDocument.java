package com.kyvc.backend.domain.corporate.domain;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 법인 단위 문서 엔티티
@Entity
@Table(name = "corporate_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorporateDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "corporate_document_id")
    private Long corporateDocumentId;

    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;

    @Column(name = "document_type_code", nullable = false, length = 100)
    private String documentTypeCode;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "document_hash", nullable = false, length = 255)
    private String documentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status_code", nullable = false, length = 50)
    private KyvcEnums.DocumentUploadStatus uploadStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "uploaded_by_type_code", nullable = false, length = 30)
    private KyvcEnums.UploadActorType uploadedByTypeCode;

    @Column(name = "uploaded_by_user_id")
    private Long uploadedByUserId;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 업로드 완료 법인 문서 생성
    public static CorporateDocument createUploaded(
            Long corporateId, // 법인 ID
            String documentTypeCode, // 문서 유형 코드
            String fileName, // 원본 파일명
            String filePath, // 저장 경로
            String mimeType, // MIME 타입
            Long fileSize, // 파일 크기
            String documentHash, // 문서 해시
            KyvcEnums.UploadActorType uploadedByTypeCode, // 업로드 주체 유형
            Long uploadedByUserId // 업로드 사용자 ID
    ) {
        CorporateDocument document = new CorporateDocument();
        document.corporateId = corporateId;
        document.documentTypeCode = documentTypeCode;
        document.fileName = fileName;
        document.filePath = filePath;
        document.mimeType = mimeType;
        document.fileSize = fileSize;
        document.documentHash = documentHash;
        document.uploadStatus = KyvcEnums.DocumentUploadStatus.UPLOADED;
        document.uploadedByTypeCode = uploadedByTypeCode;
        document.uploadedByUserId = uploadedByUserId;
        document.uploadedAt = LocalDateTime.now();
        return document;
    }
}
