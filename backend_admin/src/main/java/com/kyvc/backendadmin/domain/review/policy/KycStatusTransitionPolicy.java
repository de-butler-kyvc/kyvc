package com.kyvc.backendadmin.domain.review.policy;

import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * KYC 상태 전이 가능 여부를 판단하는 정책 클래스입니다.
 */
@Component
public class KycStatusTransitionPolicy {

    private static final Map<KyvcEnums.KycStatus, Set<KyvcEnums.KycStatus>> ALLOWED_TRANSITIONS = Map.of(
            KyvcEnums.KycStatus.MANUAL_REVIEW, Set.of(
                    KyvcEnums.KycStatus.APPROVED,
                    KyvcEnums.KycStatus.REJECTED,
                    KyvcEnums.KycStatus.NEED_SUPPLEMENT
            ),
            KyvcEnums.KycStatus.NEED_SUPPLEMENT, Set.of(
                    KyvcEnums.KycStatus.MANUAL_REVIEW
            )
    );

    /**
     * 현재 상태와 목표 상태를 기준으로 KYC 상태 전이가 가능한지 검증합니다.
     *
     * <p>수동심사 API는 현재 상태가 MANUAL_REVIEW인 KYC 신청만 APPROVED, REJECTED,
     * NEED_SUPPLEMENT 상태로 전이할 수 있습니다. 허용되지 않은 전이는 400 응답으로 처리합니다.</p>
     *
     * @param currentStatus 현재 KYC 상태
     * @param targetStatus 목표 KYC 상태
     */
    public void validateTransition(KyvcEnums.KycStatus currentStatus, KyvcEnums.KycStatus targetStatus) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new ApiException(
                    ErrorCode.INVALID_KYC_STATUS_TRANSITION,
                    "현재 상태 %s에서는 %s 상태로 변경할 수 없습니다.".formatted(currentStatus, targetStatus)
            );
        }
    }
}
