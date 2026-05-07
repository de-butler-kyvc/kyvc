package com.kyvc.backendadmin.global.commoncode.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * common_codes 테이블과 매핑되는 공통코드 엔티티입니다.
 */
@Entity
@Table(name = "common_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Long codeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "code_group_id", nullable = false)
    private CommonCodeGroup codeGroup;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "code_name", nullable = false)
    private String codeName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "enabled_yn", nullable = false, length = 1)
    private String enabledYn;

    @Column(name = "system_yn", nullable = false, length = 1)
    private String systemYn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

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

    public static CommonCode create(
            CommonCodeGroup codeGroup,
            String code,
            String codeName,
            String description,
            Integer sortOrder,
            String enabledYn,
            String systemYn,
            String metadataJson,
            Long adminId
    ) {
        CommonCode commonCode = new CommonCode();
        commonCode.codeGroup = codeGroup;
        commonCode.code = code;
        commonCode.codeName = codeName;
        commonCode.description = description;
        commonCode.sortOrder = sortOrder;
        commonCode.enabledYn = enabledYn;
        commonCode.systemYn = systemYn;
        commonCode.metadataJson = metadataJson;
        commonCode.createdByAdminId = adminId;
        commonCode.updatedByAdminId = adminId;
        return commonCode;
    }

    public boolean isSystem() {
        return "Y".equals(systemYn);
    }

    public void update(
            String codeName,
            String description,
            Integer sortOrder,
            String enabledYn,
            String metadataJson,
            Long adminId
    ) {
        this.codeName = codeName;
        this.description = description;
        this.sortOrder = sortOrder;
        this.enabledYn = enabledYn;
        this.metadataJson = metadataJson;
        this.updatedByAdminId = adminId;
    }

    public void changeEnabled(String enabledYn, Long adminId) {
        this.enabledYn = enabledYn;
        this.updatedByAdminId = adminId;
    }
}
