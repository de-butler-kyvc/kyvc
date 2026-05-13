from datetime import date

from app.ai_assessment.enums import DelegationResult
from app.ai_assessment.engines.normalizer import Normalizer
from app.ai_assessment.schemas import (
    DelegationAuthority,
    DelegationResultModel,
    EngineIssue,
    PowerOfAttorneyExtraction,
    SealCertificateExtraction,
)


class DelegationAuthorityEngine:
    def __init__(self, normalizer: Normalizer | None = None, today: date | None = None) -> None:
        self.normalizer = normalizer or Normalizer()
        self.today = today or date.today()

    def evaluate(
        self,
        poa: PowerOfAttorneyExtraction | None,
        representative_name: str | None,
        applicant_name: str | None,
        corporate_name: str | None,
        seal_certificate: SealCertificateExtraction | None = None,
    ) -> DelegationResultModel:
        if poa is None:
            return DelegationResultModel(
                result=DelegationResult.NOT_AUTHORIZED,
                reasons=[EngineIssue(code="POWER_OF_ATTORNEY_MISSING", message="Power of attorney is required for delegate applications.")],
            )

        reasons: list[EngineIssue] = []
        refs = self._refs(poa)
        self._compare_person("POA_DELEGATOR_REPRESENTATIVE_MISMATCH", poa.delegatorName.normalized, representative_name, reasons, poa.delegatorName.evidenceRefs)
        if applicant_name:
            self._compare_person("POA_DELEGATE_APPLICANT_MISMATCH", poa.delegateName.normalized, applicant_name, reasons, poa.delegateName.evidenceRefs)
        else:
            reasons.append(
                EngineIssue(
                    code="POA_APPLICANT_NAME_NOT_PROVIDED",
                    message="Applicant name is unavailable; delegate identity needs review.",
                    evidenceRefs=poa.delegateName.evidenceRefs,
                )
            )

        if corporate_name and poa.targetCorporateName.normalized and corporate_name != poa.targetCorporateName.normalized:
            reasons.append(
                EngineIssue(
                    code="POA_TARGET_CORPORATE_NAME_MISMATCH",
                    message="POA target corporate name differs from submitted corporate name.",
                    evidenceRefs=poa.targetCorporateName.evidenceRefs,
                )
            )

        kyc = bool(poa.canApplyKyc.normalized)
        submit = bool(poa.canSubmitDocuments.normalized)
        vc = bool(poa.canReceiveVc.normalized)
        if not kyc:
            reasons.append(EngineIssue(code="POA_KYC_APPLICATION_AUTHORITY_MISSING", message="KYC application authority is not explicit.", evidenceRefs=poa.authorityText.evidenceRefs))
        if not submit:
            reasons.append(EngineIssue(code="POA_DOCUMENT_SUBMISSION_AUTHORITY_MISSING", message="Document submission authority is not explicit.", evidenceRefs=poa.authorityText.evidenceRefs))
        if not vc:
            reasons.append(EngineIssue(code="POA_VC_RECEIPT_AUTHORITY_MISSING", message="VC receipt authority is not explicit.", evidenceRefs=poa.authorityText.evidenceRefs))
        if poa.validUntil.normalized and poa.validUntil.normalized < self.today.isoformat():
            reasons.append(EngineIssue(code="POA_EXPIRED", message="POA validity period has expired.", evidenceRefs=poa.validUntil.evidenceRefs))
        if not poa.hasSignatureOrSeal.normalized:
            reasons.append(EngineIssue(code="POA_SIGNATURE_OR_SEAL_MISSING", message="Signature or seal evidence is missing.", evidenceRefs=poa.hasSignatureOrSeal.evidenceRefs))
        self._validate_seal_certificate(poa, seal_certificate, representative_name, corporate_name, reasons)

        if not reasons and kyc and submit and vc:
            result = DelegationResult.AUTHORIZED
        elif kyc or submit or vc:
            result = DelegationResult.PARTIALLY_AUTHORIZED
        else:
            result = DelegationResult.MANUAL_REVIEW_REQUIRED
        return DelegationResultModel(
            result=result,
            manualReviewRequired=bool(reasons),
            reasons=reasons,
            authority=DelegationAuthority(kycApplication=kyc, documentSubmission=submit, vcReceipt=vc),
            evidenceRefs=refs,
        )

    def _compare_person(self, code: str, left: str | None, right_raw: str | None, reasons: list[EngineIssue], refs: list[str]) -> None:
        right = self.normalizer.person_name(right_raw)
        if left and right and left != right:
            reasons.append(EngineIssue(code=code, message="POA person name does not match expected value.", evidenceRefs=refs))

    def _validate_seal_certificate(
        self,
        poa: PowerOfAttorneyExtraction,
        seal_certificate: SealCertificateExtraction | None,
        representative_name: str | None,
        corporate_name: str | None,
        reasons: list[EngineIssue],
    ) -> None:
        if seal_certificate is None:
            reasons.append(
                EngineIssue(
                    code="SEAL_CERTIFICATE_MISSING",
                    message="Seal certificate is required to validate delegate power of attorney.",
                    evidenceRefs=poa.hasSignatureOrSeal.evidenceRefs,
                )
            )
            return
        self._compare_person(
            "SEAL_CERTIFICATE_SUBJECT_REPRESENTATIVE_MISMATCH",
            seal_certificate.subjectName.normalized,
            representative_name,
            reasons,
            seal_certificate.subjectName.evidenceRefs,
        )
        if corporate_name and seal_certificate.corporateName.normalized and corporate_name != seal_certificate.corporateName.normalized:
            reasons.append(
                EngineIssue(
                    code="SEAL_CERTIFICATE_CORPORATE_NAME_MISMATCH",
                    message="Seal certificate corporate name differs from submitted corporate name.",
                    evidenceRefs=seal_certificate.corporateName.evidenceRefs,
                )
            )
        poa_seal = poa.sealImpressionId.normalized or poa.sealImpressionId.raw
        cert_seal = seal_certificate.sealImpressionId.normalized or seal_certificate.sealImpressionId.raw
        if not poa_seal:
            reasons.append(EngineIssue(code="POA_SEAL_IMPRESSION_UNREADABLE", message="POA seal impression could not be compared to seal certificate.", evidenceRefs=poa.sealImpressionId.evidenceRefs))
        elif not cert_seal:
            reasons.append(EngineIssue(code="SEAL_CERTIFICATE_IMPRESSION_UNREADABLE", message="Seal certificate impression could not be compared to POA.", evidenceRefs=seal_certificate.sealImpressionId.evidenceRefs))
        elif str(poa_seal).strip() != str(cert_seal).strip():
            reasons.append(
                EngineIssue(
                    code="POA_SEAL_CERTIFICATE_MISMATCH",
                    message="POA seal impression does not match the seal certificate.",
                    evidenceRefs=poa.sealImpressionId.evidenceRefs + seal_certificate.sealImpressionId.evidenceRefs,
                )
            )

    def _refs(self, poa: PowerOfAttorneyExtraction) -> list[str]:
        refs = []
        for field in poa.model_dump().values():
            if isinstance(field, dict):
                refs.extend(field.get("evidenceRefs", []))
        return list(dict.fromkeys(refs))
