package com.kyvc.backend.domain.kyc.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.kyc.dto.KycAiReviewDetailResponse;
import com.kyvc.backend.domain.kyc.repository.KycAiReviewQueryRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycAiReviewQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long KYC_ID = 10L;
    private static final Long CORPORATE_ID = 20L;

    @Mock
    private KycAiReviewQueryRepository kycAiReviewQueryRepository;

    private ObjectMapper objectMapper;
    private KycAiReviewQueryService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new KycAiReviewQueryService(kycAiReviewQueryRepository, objectMapper);
    }

    @Test
    void getDetail_returnsOwnedAiReviewResultWithoutCoreRawFields() throws Exception {
        when(kycAiReviewQueryRepository.findByKycId(KYC_ID)).thenReturn(Optional.of(completedRow()));

        KycAiReviewDetailResponse response = service.getDetail(USER_ID, KYC_ID);

        assertThat(response.kycId()).isEqualTo(KYC_ID);
        assertThat(response.applicationStatusCode()).isEqualTo(KyvcEnums.KycStatus.MANUAL_REVIEW.name());
        assertThat(response.aiReviewStatusCode()).isEqualTo(KyvcEnums.AiReviewStatus.SUCCESS.name());
        assertThat(response.overallResultCode()).isEqualTo(KyvcEnums.AiReviewResult.NEED_MANUAL_REVIEW.name());
        assertThat(response.documentResults()).hasSize(1);
        assertThat(response.mismatchResults()).hasSize(1);
        assertThat(response.beneficialOwnerResults()).hasSize(1);
        assertThat(response.delegationResult()).isNotNull();
        assertThat(response.reviewReasons()).contains("대표자 정보 교차 검증 필요");

        String json = objectMapper.writeValueAsString(response);
        assertThat(json)
                .doesNotContain("coreRequestId")
                .doesNotContain("coreAiReviewRawJson")
                .doesNotContain("rawPayload");
    }

    @Test
    void getDetail_rejectsOtherUserKyc() {
        when(kycAiReviewQueryRepository.findByKycId(KYC_ID)).thenReturn(Optional.of(completedRow()));

        assertThatThrownBy(() -> service.getDetail(OTHER_USER_ID, KYC_ID))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.KYC_ACCESS_DENIED);
    }

    @Test
    void getDetail_throwsNotFoundWhenKycMissing() {
        when(kycAiReviewQueryRepository.findByKycId(KYC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(USER_ID, KYC_ID))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.KYC_NOT_FOUND);
    }

    @Test
    void getDetail_returnsEmptyDetailsWhenAiReviewRunning() {
        when(kycAiReviewQueryRepository.findByKycId(KYC_ID)).thenReturn(Optional.of(runningRow()));

        KycAiReviewDetailResponse response = service.getDetail(USER_ID, KYC_ID);

        assertThat(response.applicationStatusCode()).isEqualTo(KyvcEnums.KycStatus.AI_REVIEWING.name());
        assertThat(response.aiReviewStatusCode()).isEqualTo(KyvcEnums.AiReviewStatus.RUNNING.name());
        assertThat(response.overallResultCode()).isNull();
        assertThat(response.documentResults()).isEmpty();
        assertThat(response.mismatchResults()).isEmpty();
        assertThat(response.beneficialOwnerResults()).isEmpty();
        assertThat(response.delegationResult()).isNull();
        assertThat(response.reviewReasons()).isEmpty();
    }

    private KycAiReviewQueryRepository.Row completedRow() {
        return new KycAiReviewQueryRepository.Row(
                KYC_ID,
                CORPORATE_ID,
                USER_ID,
                USER_ID,
                KyvcEnums.KycStatus.MANUAL_REVIEW.name(),
                KyvcEnums.AiReviewStatus.SUCCESS.name(),
                KyvcEnums.AiReviewResult.NEED_MANUAL_REVIEW.name(),
                new BigDecimal("0.82"),
                "일부 문서 간 대표자 정보 확인이 필요합니다.",
                """
                        {
                          "coreRequestId": "core-request-id",
                          "claims": {
                            "delegation": {"status": "VALID"}
                          }
                        }
                        """,
                """
                        {
                          "documentResults": [
                            {
                              "documentId": 100,
                              "documentTypeCode": "BUSINESS_REGISTRATION",
                              "documentTypeName": "사업자등록증",
                              "resultCode": "PASSED",
                              "confidenceScore": 0.95,
                              "message": "사업자등록증 주요 정보가 정상 확인되었습니다."
                            }
                          ],
                          "crossDocumentChecks": [
                            {
                              "fieldName": "representativeName",
                              "sourceDocumentTypeCode": "BUSINESS_REGISTRATION",
                              "targetDocumentTypeCode": "CORPORATE_REGISTRY",
                              "severityCode": "WARN",
                              "message": "대표자명 교차 확인이 필요합니다."
                            }
                          ],
                          "beneficialOwnership": {
                            "owners": [
                              {
                                "name": "홍길동",
                                "ownershipPercent": 60.0,
                                "resultCode": "PASSED",
                                "message": "실소유자 기준을 충족합니다."
                              }
                            ]
                          },
                          "delegation": {
                            "resultCode": "PASSED",
                            "message": "위임권한이 확인되었습니다."
                          },
                          "manualReviewReasons": [
                            {"message": "대표자 정보 교차 검증 필요"}
                          ]
                        }
                        """,
                "대표자 정보 교차 검증 필요",
                "MANUAL_APPROVAL_REQUIRED",
                null,
                null,
                LocalDateTime.of(2026, 5, 16, 21, 30)
        );
    }

    private KycAiReviewQueryRepository.Row runningRow() {
        return new KycAiReviewQueryRepository.Row(
                KYC_ID,
                CORPORATE_ID,
                USER_ID,
                USER_ID,
                KyvcEnums.KycStatus.AI_REVIEWING.name(),
                KyvcEnums.AiReviewStatus.RUNNING.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 5, 16, 21, 0)
        );
    }
}
