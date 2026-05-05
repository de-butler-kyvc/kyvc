package com.kyvc.backend.domain.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// KYC 보완요청 문서 매핑 엔티티
@Entity
@Table(name = "kyc_supplement_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KycSupplementDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplement_document_id")
    private Long supplementDocumentId;

    @Column(name = "supplement_id", nullable = false)
    private Long supplementId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 보완요청 문서 매핑 생성
    public static KycSupplementDocument create(
            Long supplementId, // 보완요청 ID
            Long documentId // 문서 ID
    ) {
        KycSupplementDocument supplementDocument = new KycSupplementDocument();
        supplementDocument.supplementId = supplementId;
        supplementDocument.documentId = documentId;
        return supplementDocument;
    }
}
