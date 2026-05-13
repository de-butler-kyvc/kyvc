package com.kyvc.backend.domain.auth.application;

import com.kyvc.backend.domain.auth.domain.TermsConsent;
import com.kyvc.backend.domain.auth.dto.TermsConsentItemRequest;
import com.kyvc.backend.domain.auth.dto.TermsConsentItemResponse;
import com.kyvc.backend.domain.auth.dto.TermsConsentRequest;
import com.kyvc.backend.domain.auth.dto.TermsConsentResponse;
import com.kyvc.backend.domain.auth.repository.TermsConsentRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

// 약관 동의 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class TermsConsentService {

    private static final Set<KyvcEnums.ConsentType> REQUIRED_TERMS_TYPES = Set.of(
            KyvcEnums.ConsentType.TERMS_OF_SERVICE,
            KyvcEnums.ConsentType.PRIVACY_POLICY,
            KyvcEnums.ConsentType.KYC_PROCESSING
    ); // 필수 약관 유형 목록

    private final TermsConsentRepository termsConsentRepository;

    // 내 약관 동의 상태 조회
    @Transactional(readOnly = true)
    public TermsConsentResponse getMyTermsConsent(
            Long userId // 사용자 ID
    ) {
        validateUserId(userId);
        return buildResponse(userId);
    }

    // 약관 동의 저장
    public TermsConsentResponse saveMyTermsConsent(
            Long userId, // 사용자 ID
            TermsConsentRequest request // 약관 동의 요청
    ) {
        validateUserId(userId);
        validateConsentRequest(request);
        validateConsentItems(request.consents());

        LocalDateTime now = LocalDateTime.now(); // 동의 처리 기준 시각

        for (TermsConsentItemRequest item : request.consents()) {
            KyvcEnums.ConsentType consentType = parseConsentType(item.termsCode()); // 약관 유형
            String termsCode = consentType.name(); // 서버 기준 약관 코드
            String termsVersion = normalize(item.termsVersion()); // 약관 버전
            boolean requiredTerms = isRequiredTermsType(consentType); // 서버 기준 필수 약관 여부

            if (requiredTerms && Boolean.FALSE.equals(item.agreed())) {
                throw new ApiException(ErrorCode.INVALID_REQUEST);
            }

            TermsConsent termsConsent = termsConsentRepository
                    .findByUserIdAndTermsCodeAndTermsVersion(userId, termsCode, termsVersion)
                    .orElseGet(() -> TermsConsent.create(
                            userId,
                            termsCode,
                            termsVersion,
                            item.agreed(),
                            now
                    ));

            if (termsConsent.getId() != null) {
                termsConsent.update(item.agreed(), now);
            }

            termsConsentRepository.save(termsConsent);
        }

        return buildResponse(userId);
    }

    // 사용자 기준 응답 생성
    private TermsConsentResponse buildResponse(
            Long userId // 사용자 ID
    ) {
        List<TermsConsent> consents = termsConsentRepository.findByUserId(userId);
        if (consents.isEmpty()) {
            return new TermsConsentResponse(userId, false, List.of());
        }

        List<TermsConsent> requiredConsents = consents.stream()
                .filter(termsConsent -> isRequiredTermsCode(termsConsent.getTermsCode()))
                .toList();
        boolean allRequiredAgreed = !requiredConsents.isEmpty()
                && requiredConsents.stream().allMatch(TermsConsent::isAgreed);

        List<TermsConsentItemResponse> responseItems = consents.stream()
                .map(this::toResponseItem)
                .toList();

        return new TermsConsentResponse(userId, allRequiredAgreed, responseItems);
    }

    // 약관 동의 요청 검증
    private void validateConsentRequest(
            TermsConsentRequest request // 약관 동의 요청
    ) {
        if (request == null || CollectionUtils.isEmpty(request.consents())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 약관 동의 항목 목록 검증
    private void validateConsentItems(
            List<TermsConsentItemRequest> consents // 약관 동의 목록
    ) {
        for (TermsConsentItemRequest consent : consents) {
            validateConsentItem(consent);
        }
    }

    // 약관 동의 항목 검증
    private void validateConsentItem(
            TermsConsentItemRequest consent // 약관 동의 항목
    ) {
        if (consent == null
                || !StringUtils.hasText(consent.termsCode())
                || !StringUtils.hasText(consent.termsVersion())
                || consent.required() == null
                || consent.agreed() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        parseConsentType(consent.termsCode());
    }

    // 약관 동의 응답 항목 변환
    private TermsConsentItemResponse toResponseItem(
            TermsConsent termsConsent // 약관 동의 이력
    ) {
        return new TermsConsentItemResponse(
                termsConsent.getTermsCode(),
                termsConsent.getTermsVersion(),
                isRequiredTermsCode(termsConsent.getTermsCode()),
                termsConsent.isAgreed(),
                termsConsent.getAgreedAt()
        );
    }

    // 약관 유형 파싱
    private KyvcEnums.ConsentType parseConsentType(
            String termsCode // 약관 코드
    ) {
        try {
            return KyvcEnums.ConsentType.valueOf(normalize(termsCode));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 필수 약관 코드 여부
    private boolean isRequiredTermsCode(
            String termsCode // 약관 코드
    ) {
        return isRequiredTermsType(parseConsentType(termsCode));
    }

    // 필수 약관 유형 여부
    private boolean isRequiredTermsType(
            KyvcEnums.ConsentType consentType // 약관 유형
    ) {
        return REQUIRED_TERMS_TYPES.contains(consentType);
    }

    // 문자열 정규화
    private String normalize(
            String value // 원본 문자열
    ) {
        return value.trim();
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
