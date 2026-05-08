package com.kyvc.backend.domain.auth.application;

import com.kyvc.backend.domain.auth.dto.SessionResponse;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.user.domain.User;
import com.kyvc.backend.domain.user.repository.UserRepository;
import com.kyvc.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// 세션 조회 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SessionService {

    private final CorporateRepository corporateRepository;
    private final UserRepository userRepository;

    // 현재 세션 정보 조회
    public SessionResponse getSession(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null) {
            return new SessionResponse(null, null, false, null, null, null, null, List.of(), null, false);
        }

        Optional<Corporate> corporate = corporateRepository.findByUserId(userDetails.getUserId());
        User user = userRepository.findById(userDetails.getUserId()).orElse(null); // 세션 표시용 사용자 정보
        String userName = userRepository.findById(userDetails.getUserId())
                .map(User::getUserName)
                .orElse(null); // 사용자명

        return new SessionResponse(
                user == null ? null : user.getOnboardingCorporateName(),
                user == null ? null : user.getUserStatusCode().name(),
                true,
                userDetails.getUserId(),
                userDetails.getEmail(),
                userName,
                userDetails.getUserType(),
                userDetails.getRoles(),
                corporate.map(Corporate::getCorporateId).orElse(null),
                corporate.isPresent()
        );
    }
}
