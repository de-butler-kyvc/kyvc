package com.kyvc.backend.domain.kyc.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.kyc.dto.KycAiReviewDetailResponse;
import com.kyvc.backend.domain.kyc.repository.KycAiReviewQueryRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// KYC AI 심사 결과 상세 조회 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KycAiReviewQueryService {

    private final KycAiReviewQueryRepository kycAiReviewQueryRepository;
    private final ObjectMapper objectMapper;

    // KYC AI 심사 결과 상세 조회
    public KycAiReviewDetailResponse getDetail(
            Long userId, // 사용자 ID
            Long kycId // KYC 신청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);

        KycAiReviewQueryRepository.Row row = kycAiReviewQueryRepository.findByKycId(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        validateOwnership(userId, row);

        JsonNode assessmentNode = parseJson(row.coreAiAssessmentJson()); // 안전한 AI assessment JSON
        JsonNode detailNode = parseJson(row.aiReviewDetailJson()); // 저장된 AI 상세 JSON
        KyvcEnums.KycStatus applicationStatus = parseKycStatus(row.applicationStatusCode()); // KYC 신청 상태
        KyvcEnums.AiReviewResult aiReviewResult = parseAiReviewResult(row.aiReviewResultCode()); // AI 심사 결과

        return new KycAiReviewDetailResponse(
                row.kycId(),
                row.applicationStatusCode(),
                resolveAiReviewStatusCode(row, applicationStatus),
                row.aiReviewResultCode(),
                row.aiConfidenceScore(),
                resolveReviewedAt(row, assessmentNode),
                isManualReviewRequired(applicationStatus, aiReviewResult, row.manualReviewReason()),
                KyvcEnums.KycStatus.NEED_SUPPLEMENT == applicationStatus,
                row.aiReviewSummary(),
                buildDocumentResults(assessmentNode),
                buildMismatchResults(assessmentNode),
                buildBeneficialOwnerResults(assessmentNode, detailNode),
                buildDelegationResult(assessmentNode, detailNode),
                buildReviewReasons(row, assessmentNode)
        );
    }

    // 소유권 검증
    private void validateOwnership(
            Long userId, // 사용자 ID
            KycAiReviewQueryRepository.Row row // KYC AI 심사 조회 행
    ) {
        if (Objects.equals(userId, row.applicantUserId())
                || Objects.equals(userId, row.corporateUserId())) {
            return;
        }
        throw new ApiException(ErrorCode.KYC_ACCESS_DENIED);
    }

    // AI 심사 상태 코드 결정
    private String resolveAiReviewStatusCode(
            KycAiReviewQueryRepository.Row row, // KYC AI 심사 조회 행
            KyvcEnums.KycStatus applicationStatus // KYC 신청 상태
    ) {
        if (StringUtils.hasText(row.aiReviewStatusCode())) {
            return row.aiReviewStatusCode();
        }
        if (KyvcEnums.KycStatus.AI_REVIEWING == applicationStatus) {
            return KyvcEnums.AiReviewStatus.RUNNING.name();
        }
        return null;
    }

    // 심사 반영 일시 결정
    private LocalDateTime resolveReviewedAt(
            KycAiReviewQueryRepository.Row row, // KYC AI 심사 조회 행
            JsonNode assessmentNode // AI assessment JSON
    ) {
        if (!hasReviewResult(row, assessmentNode)) {
            return null;
        }
        if (row.approvedAt() != null) {
            return row.approvedAt();
        }
        if (row.rejectedAt() != null) {
            return row.rejectedAt();
        }
        return row.updatedAt();
    }

    // 심사 결과 존재 여부
    private boolean hasReviewResult(
            KycAiReviewQueryRepository.Row row, // KYC AI 심사 조회 행
            JsonNode assessmentNode // AI assessment JSON
    ) {
        return StringUtils.hasText(row.aiReviewResultCode())
                || row.aiConfidenceScore() != null
                || StringUtils.hasText(row.aiReviewSummary())
                || (assessmentNode != null && !assessmentNode.isMissingNode() && !assessmentNode.isNull());
    }

    // 수기 심사 필요 여부
    private boolean isManualReviewRequired(
            KyvcEnums.KycStatus applicationStatus, // KYC 신청 상태
            KyvcEnums.AiReviewResult aiReviewResult, // AI 심사 결과
            String manualReviewReason // 수기 심사 사유
    ) {
        return KyvcEnums.KycStatus.MANUAL_REVIEW == applicationStatus
                || KyvcEnums.AiReviewResult.NEED_MANUAL_REVIEW == aiReviewResult
                || StringUtils.hasText(manualReviewReason);
    }

    // 문서별 심사 결과 목록 생성
    private List<KycAiReviewDetailResponse.DocumentResult> buildDocumentResults(
            JsonNode assessmentNode // AI assessment JSON
    ) {
        return arrayNode(assessmentNode, "documentResults").stream()
                .map(node -> new KycAiReviewDetailResponse.DocumentResult(
                        longValue(node, "documentId", "id"),
                        textValue(node, "documentTypeCode", "documentType", "type"),
                        textValue(node, "documentTypeName", "documentName", "name"),
                        textValue(node, "resultCode", "status", "result"),
                        decimalValue(node, "confidenceScore", "confidence", "score"),
                        textValue(node, "message", "summary", "reason")
                ))
                .toList();
    }

    // 문서 간 불일치 결과 목록 생성
    private List<KycAiReviewDetailResponse.MismatchResult> buildMismatchResults(
            JsonNode assessmentNode // AI assessment JSON
    ) {
        List<JsonNode> nodes = arrayNode(assessmentNode, "crossDocumentChecks");
        if (nodes.isEmpty()) {
            nodes = arrayNode(assessmentNode, "mismatchResults");
        }
        return nodes.stream()
                .map(node -> new KycAiReviewDetailResponse.MismatchResult(
                        textValue(node, "fieldName", "field", "path"),
                        textValue(node, "sourceDocumentTypeCode", "sourceDocumentType", "sourceType"),
                        textValue(node, "targetDocumentTypeCode", "targetDocumentType", "targetType"),
                        textValue(node, "severityCode", "severity", "level"),
                        textValue(node, "message", "summary", "reason")
                ))
                .toList();
    }

    // 실소유자 심사 결과 목록 생성
    private List<KycAiReviewDetailResponse.BeneficialOwnerResult> buildBeneficialOwnerResults(
            JsonNode assessmentNode, // AI assessment JSON
            JsonNode detailNode // 저장된 AI 상세 JSON
    ) {
        List<JsonNode> nodes = arrayNode(childNode(assessmentNode, "beneficialOwnership"), "owners");
        if (nodes.isEmpty()) {
            nodes = arrayNode(assessmentNode, "beneficialOwnerResults");
        }
        if (nodes.isEmpty()) {
            nodes = arrayNode(childNode(childNode(detailNode, "claims"), "beneficialOwners"));
        }
        return nodes.stream()
                .map(node -> new KycAiReviewDetailResponse.BeneficialOwnerResult(
                        textValue(node, "ownerName", "name"),
                        decimalValue(node, "ownershipRatio", "ownershipPercent", "ownershipPercentage"),
                        textValue(node, "resultCode", "status", "result"),
                        textValue(node, "message", "summary", "reason")
                ))
                .toList();
    }

    // 위임권한 심사 결과 생성
    private KycAiReviewDetailResponse.DelegationResult buildDelegationResult(
            JsonNode assessmentNode, // AI assessment JSON
            JsonNode detailNode // 저장된 AI 상세 JSON
    ) {
        JsonNode delegationNode = childNode(assessmentNode, "delegation");
        if (delegationNode == null || delegationNode.isMissingNode() || delegationNode.isNull() || delegationNode.isEmpty()) {
            delegationNode = childNode(childNode(detailNode, "claims"), "delegation");
        }
        if (delegationNode == null || delegationNode.isMissingNode() || delegationNode.isNull() || delegationNode.isEmpty()) {
            return null;
        }
        return new KycAiReviewDetailResponse.DelegationResult(
                textValue(delegationNode, "resultCode", "status", "result"),
                textValue(delegationNode, "message", "summary", "reason")
        );
    }

    // 심사 사유 목록 생성
    private List<String> buildReviewReasons(
            KycAiReviewQueryRepository.Row row, // KYC AI 심사 조회 행
            JsonNode assessmentNode // AI assessment JSON
    ) {
        Set<String> reviewReasons = new LinkedHashSet<>();
        addText(reviewReasons, row.manualReviewReason());
        addText(reviewReasons, row.aiReviewReasonCode());
        addIssueReasons(reviewReasons, assessmentNode, "manualReviewReasons");
        addIssueReasons(reviewReasons, assessmentNode, "supplementRequests");
        return List.copyOf(reviewReasons);
    }

    // 이슈 사유 추가
    private void addIssueReasons(
            Set<String> reviewReasons, // 심사 사유 목록
            JsonNode assessmentNode, // AI assessment JSON
            String fieldName // 이슈 목록 필드명
    ) {
        for (JsonNode issueNode : arrayNode(assessmentNode, fieldName)) {
            String message = textValue(issueNode, "message", "reason", "code");
            addText(reviewReasons, message);
        }
    }

    // 텍스트 추가
    private void addText(
            Set<String> values, // 대상 문자열 목록
            String value // 추가 문자열
    ) {
        if (StringUtils.hasText(value)) {
            values.add(value.trim());
        }
    }

    // JSON 파싱
    private JsonNode parseJson(
            String json // 원본 JSON
    ) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    // 배열 노드 조회
    private List<JsonNode> arrayNode(
            JsonNode parent, // 부모 JSON
            String fieldName // 배열 필드명
    ) {
        JsonNode node = childNode(parent, fieldName);
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<JsonNode> nodes = new ArrayList<>();
        node.forEach(nodes::add);
        return List.copyOf(nodes);
    }

    // 배열 노드 변환
    private List<JsonNode> arrayNode(
            JsonNode node // 배열 JSON
    ) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<JsonNode> nodes = new ArrayList<>();
        node.forEach(nodes::add);
        return List.copyOf(nodes);
    }

    // 하위 노드 조회
    private JsonNode childNode(
            JsonNode parent, // 부모 JSON
            String fieldName // 필드명
    ) {
        if (parent == null || parent.isMissingNode() || parent.isNull()) {
            return null;
        }
        return parent.path(fieldName);
    }

    // 텍스트 값 조회
    private String textValue(
            JsonNode node, // JSON 노드
            String... fieldNames // 후보 필드명 목록
    ) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull() && StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        return null;
    }

    // Long 값 조회
    private Long longValue(
            JsonNode node, // JSON 노드
            String... fieldNames // 후보 필드명 목록
    ) {
        String value = textValue(node, fieldNames); // 원본 값
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    // BigDecimal 값 조회
    private BigDecimal decimalValue(
            JsonNode node, // JSON 노드
            String... fieldNames // 후보 필드명 목록
    ) {
        String value = textValue(node, fieldNames); // 원본 값
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    // KYC 상태 변환
    private KyvcEnums.KycStatus parseKycStatus(
            String statusCode // KYC 상태 코드
    ) {
        if (!StringUtils.hasText(statusCode)) {
            return null;
        }
        try {
            return KyvcEnums.KycStatus.valueOf(statusCode);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    // AI 심사 결과 변환
    private KyvcEnums.AiReviewResult parseAiReviewResult(
            String resultCode // AI 심사 결과 코드
    ) {
        if (!StringUtils.hasText(resultCode)) {
            return null;
        }
        try {
            return KyvcEnums.AiReviewResult.valueOf(resultCode);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // KYC 신청 ID 검증
    private void validateKycId(
            Long kycId // KYC 신청 ID
    ) {
        if (kycId == null || kycId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }
}
