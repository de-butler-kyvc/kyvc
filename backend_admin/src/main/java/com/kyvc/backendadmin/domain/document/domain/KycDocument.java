package com.kyvc.backendadmin.domain.document.domain;

import com.kyvc.backendadmin.global.util.KyvcEnums;
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

/**
 * kyc_documents 테이블과 매핑되는 문서 엔티티입니다.
 *
 * <p>KYC 신청에 제출된 문서의 유형, 원본 파일명, 내부 저장 경로, MIME 타입,
 * 파일 크기, 문서 해시, 업로드 상태와 업로드 시각을 보관합니다.</p>
 */
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

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "document_hash", nullable = false)
    private String documentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status_code", nullable = false, length = 50)
    private KyvcEnums.DocumentUploadStatus uploadStatusCode;

    @Column(name = "uploaded_by_type_code", length = 30)
    private String uploadedByTypeCode;

    @Column(name = "uploaded_by_user_id")
    private Long uploadedByUserId;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
