package com.kyvc.backend.domain.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import org.hibernate.type.YesNoConverter;

import java.time.LocalDateTime;

// 법인 유형별 제출 문서 요구사항 Entity
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

    @Convert(converter = YesNoConverter.class)
    @Column(name = "required_yn", nullable = false, length = 1)
    private Boolean required;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "enabled_yn", nullable = false, length = 1)
    private Boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "guide_message", columnDefinition = "TEXT")
    private String guideMessage;

    @Column(name = "requirement_group_code", length = 100)
    private String requirementGroupCode;

    @Column(name = "requirement_group_name", length = 200)
    private String requirementGroupName;

    @Column(name = "min_required_count")
    private Integer minRequiredCount;

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

    // 단일 필수 문서 여부
    public boolean isRequired() {
        return Boolean.TRUE.equals(required);
    }

    // 활성 정책 여부
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}
