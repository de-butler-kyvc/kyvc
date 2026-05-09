package com.kyvc.backend.domain.corporate.application;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.domain.CorporateDocument;
import com.kyvc.backend.domain.corporate.domain.CorporateRepresentative;
import com.kyvc.backend.domain.corporate.dto.RepresentativeRequest;
import com.kyvc.backend.domain.corporate.dto.RepresentativeResponse;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepresentativeRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 대표자 정보 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CorporateRepresentativeService {

    private static final String REPRESENTATIVE_ID_DOCUMENT_TYPE = "REPRESENTATIVE_ID"; // 대표자 신분증 문서 유형 코드

    private final CorporateRepository corporateRepository;
    private final CorporateRepresentativeRepository corporateRepresentativeRepository;
    private final CorporateDocumentService corporateDocumentService;

    // 대표자 정보 저장
    public RepresentativeResponse saveRepresentative(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            RepresentativeRequest request // 대표자 정보 저장 요청
    ) {
        validateUserId(userId);
        validateCorporateId(corporateId);
        validateRepresentativeRequest(request);

        Corporate corporate = getOwnedCorporate(userId, corporateId);
        String representativeName = resolveRepresentativeName(corporate, request); // 대표자명
        String nationalityCode = normalizeRequired(request.nationalityCode()); // 대표자 국적 코드
        Long identityDocumentId = storeRequiredIdentityDocument(userId, corporateId, request.identityFile()); // 신분증 문서 ID
        CorporateRepresentative representative = corporateRepresentativeRepository.findByCorporateId(corporateId)
                .map(existing -> {
                    existing.update(
                            representativeName,
                            request.birthDate(),
                            nationalityCode,
                            normalizeOptional(request.phoneNumber()),
                            normalizeOptional(request.email()),
                            identityDocumentId
                    );
                    return existing;
                })
                .orElseGet(() -> CorporateRepresentative.create(
                        corporateId,
                        representativeName,
                        request.birthDate(),
                        nationalityCode,
                        normalizeOptional(request.phoneNumber()),
                        normalizeOptional(request.email()),
                        identityDocumentId
                ));

        CorporateRepresentative savedRepresentative = saveRepresentativeEntity(representative); // 저장된 대표자
        syncLegacyCorporateRepresentative(corporate, savedRepresentative);
        return toResponse(savedRepresentative);
    }

    // 대표자 정보 목록 조회
    @Transactional(readOnly = true)
    public List<RepresentativeResponse> getRepresentatives(
            Long userId, // 사용자 ID
            Long corporateId // 법인 ID
    ) {
        validateUserId(userId);
        validateCorporateId(corporateId);

        Corporate corporate = getOwnedCorporate(userId, corporateId);
        return corporateRepresentativeRepository.findByCorporateId(corporateId)
                .map(representative -> List.of(toResponse(representative)))
                .orElseGet(() -> legacyRepresentativeResponse(corporate));
    }

    // 대표자 정보 수정
    public RepresentativeResponse updateRepresentative(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            Long representativeId, // 대표자 ID
            RepresentativeRequest request // 대표자 정보 수정 요청
    ) {
        validateUserId(userId);
        validateCorporateId(corporateId);
        validateRepresentativeId(representativeId);
        validateRepresentativeRequest(request);

        Corporate corporate = getOwnedCorporate(userId, corporateId);
        CorporateRepresentative representative = corporateRepresentativeRepository.findById(representativeId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        validateRepresentativeRelation(corporateId, representative);

        String representativeName = resolveRepresentativeName(corporate, request); // 대표자명
        String nationalityCode = normalizeRequired(request.nationalityCode()); // 대표자 국적 코드
        Long identityDocumentId = storeIdentityDocument(userId, corporateId, request.identityFile()); // 신분증 문서 ID
        representative.update(
                representativeName,
                request.birthDate(),
                nationalityCode,
                normalizeOptional(request.phoneNumber()),
                normalizeOptional(request.email()),
                identityDocumentId
        );
        CorporateRepresentative savedRepresentative = saveRepresentativeEntity(representative); // 저장된 대표자
        syncLegacyCorporateRepresentative(corporate, savedRepresentative);
        return toResponse(savedRepresentative);
    }

    // 대표자 정보 저장 요청 검증
    private void validateRepresentativeRequest(
            RepresentativeRequest request // 대표자 정보 저장 요청
    ) {
        if (request == null || !StringUtils.hasText(request.nationalityCode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 대표자명 결정
    private String resolveRepresentativeName(
            Corporate corporate, // 법인 엔티티
            RepresentativeRequest request // 대표자 정보 저장 요청
    ) {
        if (StringUtils.hasText(request.name())) {
            return normalizeRequired(request.name());
        }
        if (StringUtils.hasText(corporate.getRepresentativeName())) {
            return corporate.getRepresentativeName();
        }
        throw new ApiException(ErrorCode.INVALID_REQUEST);
    }

    // 대표자와 법인 관계 검증
    private void validateRepresentativeRelation(
            Long corporateId, // 법인 ID
            CorporateRepresentative representative // 대표자 엔티티
    ) {
        if (!representative.belongsToCorporate(corporateId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 소유 법인 조회
    private Corporate getOwnedCorporate(
            Long userId, // 사용자 ID
            Long corporateId // 법인 ID
    ) {
        Corporate corporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        if (!corporate.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.CORPORATE_ACCESS_DENIED);
        }
        return corporate;
    }

    // 대표자 응답 변환
    private RepresentativeResponse toResponse(
            CorporateRepresentative representative // 대표자 엔티티
    ) {
        return new RepresentativeResponse(
                representative.getRepresentativeId(),
                representative.getCorporateId(),
                representative.getRepresentativeName(),
                representative.getBirthDate(),
                representative.getNationalityCode(),
                representative.getPhone(),
                representative.getEmail(),
                representative.getIdentityDocumentId()
        );
    }

    // legacy 대표자 응답 변환
    private List<RepresentativeResponse> legacyRepresentativeResponse(
            Corporate corporate // 법인 엔티티
    ) {
        if (!StringUtils.hasText(corporate.getRepresentativeName())) {
            return List.of();
        }
        return List.of(new RepresentativeResponse(
                corporate.getCorporateId(),
                corporate.getCorporateId(),
                corporate.getRepresentativeName(),
                null,
                null,
                corporate.getRepresentativePhone(),
                corporate.getRepresentativeEmail(),
                null
        ));
    }

    // 대표자 Entity 저장
    private CorporateRepresentative saveRepresentativeEntity(
            CorporateRepresentative representative // 저장 대상 대표자
    ) {
        try {
            return corporateRepresentativeRepository.save(representative);
        } catch (DataAccessException exception) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    // 대표자 신분증 필수 저장
    private Long storeRequiredIdentityDocument(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            MultipartFile identityFile // 신분증 사본 파일
    ) {
        if (!hasFile(identityFile)) {
            throw new ApiException(ErrorCode.DOCUMENT_FILE_REQUIRED);
        }
        return storeIdentityDocument(userId, corporateId, identityFile);
    }

    // 대표자 신분증 저장
    private Long storeIdentityDocument(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            MultipartFile identityFile // 신분증 사본 파일
    ) {
        if (!hasFile(identityFile)) {
            return null;
        }
        CorporateDocument document = corporateDocumentService.storeCorporateDocument(
                corporateId,
                REPRESENTATIVE_ID_DOCUMENT_TYPE,
                identityFile,
                userId
        );
        return document.getCorporateDocumentId();
    }

    // legacy 법인 대표자 컬럼 동기화
    private void syncLegacyCorporateRepresentative(
            Corporate corporate, // 법인 엔티티
            CorporateRepresentative representative // 대표자 엔티티
    ) {
        corporate.updateRepresentative(
                representative.getRepresentativeName(),
                representative.getPhone(),
                representative.getEmail()
        );
        corporateRepository.save(corporate);
    }

    // 파일 포함 여부
    private boolean hasFile(
            MultipartFile file // 업로드 파일
    ) {
        return file != null && !file.isEmpty();
    }

    // 필수 문자열 정규화
    private String normalizeRequired(
            String value // 원본 문자열
    ) {
        return value.trim();
    }

    // 선택 문자열 정규화
    private String normalizeOptional(
            String value // 원본 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
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

    // 법인 ID 검증
    private void validateCorporateId(
            Long corporateId // 법인 ID
    ) {
        if (corporateId == null || corporateId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 대표자 ID 검증
    private void validateRepresentativeId(
            Long representativeId // 대표자 ID
    ) {
        if (representativeId == null || representativeId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }
}
