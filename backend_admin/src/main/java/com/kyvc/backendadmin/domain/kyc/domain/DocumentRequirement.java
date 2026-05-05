package com.kyvc.backendadmin.domain.kyc.domain;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * document_requirements 테이블과 매핑되는 정책 엔티티입니다.
 *
 * <p>법인 유형과 문서 유형 조합별 필수 제출 여부, 사용 여부, 정렬 순서,
 * 안내 문구와 생성/수정 관리자 정보를 보관합니다.</p>
 */
@Entity
@Table(name = "document_requirements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "requirement_id")
    private Long requirementId;

    @Column(name = "corporate_type_code", nullable = false, length = 50)
    private String corporateTypeCode;

    @Column(name = "document_type_code", nullable = false, length = 100)
    private String documentTypeCode;

    @Column(name = "required_yn", nullable = false, length = 1)
    private String requiredYn;

    @Column(name = "enabled_yn", nullable = false, length = 1)
    private String enabledYn;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "guide_message", columnDefinition = "TEXT")
    private String guideMessage;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "updated_by_admin_id")
    private Long updatedByAdminId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 법인 유형별 필수서류 정책을 생성합니다.
     *
     * @param corporateTypeCode 법인 유형 공통코드
     * @param documentTypeCode 문서 유형 공통코드
     * @param requiredYn 필수 여부
     * @param enabledYn 사용 여부
     * @param sortOrder 정렬 순서
     * @param guideMessage 안내 문구
     * @param adminId 생성/수정 관리자 ID
     * @return 필수서류 정책 엔티티
     */
    public static DocumentRequirement create(
            String corporateTypeCode,
            String documentTypeCode,
            String requiredYn,
            String enabledYn,
            Integer sortOrder,
            String guideMessage,
            Long adminId
    ) {
        DocumentRequirement requirement = new DocumentRequirement();
        requirement.corporateTypeCode = corporateTypeCode;
        requirement.documentTypeCode = documentTypeCode;
        requirement.requiredYn = requiredYn;
        requirement.enabledYn = enabledYn;
        requirement.sortOrder = sortOrder;
        requirement.guideMessage = guideMessage;
        requirement.createdByAdminId = adminId;
        requirement.updatedByAdminId = adminId;
        return requirement;
    }
}
