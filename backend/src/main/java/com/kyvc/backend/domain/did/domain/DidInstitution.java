package com.kyvc.backend.domain.did.domain;

import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// DID 기관 매핑 Entity
@Entity
@Table(name = "did_institutions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DidInstitution {

    @Id
    @Column(name = "did", nullable = false, length = 255)
    private String did;

    @Column(name = "institution_name", nullable = false, length = 200)
    private String institutionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 30)
    private KyvcEnums.DidInstitutionStatus statusCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 사용 가능 상태 여부
    public boolean isActive() {
        return KyvcEnums.DidInstitutionStatus.ACTIVE == statusCode;
    }
}
