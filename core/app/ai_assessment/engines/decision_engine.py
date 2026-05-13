from app.ai_assessment.enums import (
    ApplicantRole,
    AssessmentStatus,
    BeneficialOwnershipStatus,
    CheckStatus,
    DelegationResult,
    Severity,
)
from app.ai_assessment.schemas import BeneficialOwnershipResult, CrossDocumentCheck, DelegationResultModel, EngineIssue


class DecisionEngine:
    def decide(
        self,
        missing_documents: list[EngineIssue],
        checks: list[CrossDocumentCheck],
        beneficial_ownership: BeneficialOwnershipResult,
        delegation: DelegationResultModel | None,
        applicant_role: ApplicantRole,
    ) -> tuple[AssessmentStatus, list[EngineIssue], list[EngineIssue]]:
        supplement_requests = list(missing_documents)
        manual_review_reasons: list[EngineIssue] = []

        supplement_requests.extend(
            issue for issue in beneficial_ownership.issues if issue.code == "CORPORATE_SHAREHOLDER_RECURSIVE_DOCUMENT_REQUIRED"
        )
        manual_review_reasons.extend(
            issue for issue in beneficial_ownership.issues if issue.code != "CORPORATE_SHAREHOLDER_RECURSIVE_DOCUMENT_REQUIRED"
        )

        for check in checks:
            if check.status == CheckStatus.FAIL and check.severity in {Severity.CRITICAL, Severity.HIGH}:
                manual_review_reasons.append(EngineIssue(code=check.checkCode, message=check.message, evidenceRefs=check.evidenceRefs))
            elif check.status == CheckStatus.WARN:
                manual_review_reasons.append(EngineIssue(code=check.checkCode, message=check.message, evidenceRefs=check.evidenceRefs))

        if beneficial_ownership.status in {
            BeneficialOwnershipStatus.MANUAL_REVIEW_REQUIRED,
            BeneficialOwnershipStatus.UNRESOLVED,
        }:
            manual_review_reasons.append(
                EngineIssue(code="BENEFICIAL_OWNER_REVIEW_REQUIRED", message="Beneficial ownership could not be fully resolved.")
            )

        if applicant_role == ApplicantRole.DELEGATE and delegation:
            if delegation.result == DelegationResult.NOT_AUTHORIZED:
                supplement_requests.extend(delegation.reasons)
            elif delegation.result != DelegationResult.AUTHORIZED or delegation.manualReviewRequired:
                manual_review_reasons.extend(delegation.reasons)

        if supplement_requests:
            return AssessmentStatus.SUPPLEMENT_REQUIRED, supplement_requests, manual_review_reasons
        if manual_review_reasons:
            return AssessmentStatus.MANUAL_REVIEW_REQUIRED, supplement_requests, manual_review_reasons
        return AssessmentStatus.NORMAL, supplement_requests, manual_review_reasons
