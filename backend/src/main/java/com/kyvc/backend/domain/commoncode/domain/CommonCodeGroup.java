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

// 공통코드 그룹 엔티티
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

    @Convert(converter = YesNoConverter.class)
    @Column(name = "enabled_yn", nullable = false, length = 1)
    private Boolean enabled;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

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
}
