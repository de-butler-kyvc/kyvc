package com.kyvc.backend.domain.user.application;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.user.dto.UserDashboardResponse;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// 법인 사용자 대시보드 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserDashboardService {

    private final CorporateRepository corporateRepository;

    // 법인 사용자 대시보드 조회
    public UserDashboardResponse getDashboard(
            Long userId // 사용자 ID
    ) {
        validateUserId(userId);

        Optional<Corporate> corporate = corporateRepository.findByUserId(userId); // 사용자 법인정보
        return corporate
                .map(value -> new UserDashboardResponse(
                        userId,
                        true,
                        value.getCorporateId(),
                        value.getCorporateName(),
                        null,
                        null,
                        0,
                        0,
                        false
                ))
                .orElseGet(() -> new UserDashboardResponse(
                        userId,
                        false,
                        null,
                        null,
                        null,
                        null,
                        0,
                        0,
                        false
                ));
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }
}
