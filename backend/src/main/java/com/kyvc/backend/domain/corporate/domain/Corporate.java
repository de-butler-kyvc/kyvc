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

// 법인 최소 조회 엔티티
@Entity
@Table(name = "corporates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Corporate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "corporate_id")
    private Long corporateId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "corporate_status_code", nullable = false, length = 50)
    private KyvcEnums.CorporateStatus corporateStatusCode;
}
