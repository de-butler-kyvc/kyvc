package com.kyvc.backend.domain.commoncode.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import org.hibernate.type.YesNoConverter;

import java.time.LocalDateTime;

// 공통코드 상세 Entity
@Entity
@Table(name = "common_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "code_group_id", nullable = false)
    private CommonCodeGroup codeGroup;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "code_name", nullable = false, length = 255)
    private String codeName;

    @Column(name = "description")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "enabled_yn", nullable = false, length = 1)
    private Boolean enabled;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "system_yn", nullable = false, length = 1)
    private Boolean system;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 활성 코드 여부
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    // 시스템 필수 코드 여부
    public boolean isSystem() {
        return Boolean.TRUE.equals(system);
    }
}
