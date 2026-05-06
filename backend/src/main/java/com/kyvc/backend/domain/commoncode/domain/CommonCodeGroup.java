package com.kyvc.backend.domain.commoncode.domain;

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

// 공통코드 그룹 Entity
@Entity
@Table(name = "common_code_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonCodeGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_group_id")
    private Long id;

    @Column(name = "code_group", nullable = false, length = 100, unique = true)
    private String codeGroup;

    @Column(name = "code_group_name", nullable = false, length = 100)
    private String groupName;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 활성 그룹 여부
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    // 시스템 필수 그룹 여부
    public boolean isSystem() {
        return Boolean.TRUE.equals(system);
    }
}
