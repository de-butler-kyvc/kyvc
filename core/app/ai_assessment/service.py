from dataclasses import dataclass
from typing import Any, Iterable

from app.ai_assessment.engines import (
    BeneficialOwnerEngine,
    CrossDocumentConsistencyChecker,
    DecisionEngine,
    DelegationAuthorityEngine,
    RequiredDocumentsEngine,
)
from app.ai_assessment.enums import ApplicantRole, CheckStatus, DocumentType, Severity
from app.ai_assessment.extractors import StructuredPayloadExtractor
from app.ai_assessment.providers import DocumentExtractionProvider
from app.ai_assessment.schemas import (
    BeneficialOwner,
    BusinessRegistrationExtraction,
    CorporateRegistryExtraction,
    CrossDocumentCheck,
    DeclaredBeneficialOwner,
    DocumentMetadata,
    DocumentResult,
    EngineIssue,
    KycApplication,
    KycAssessment,
    ModelMetadata,
    OrganizationDocumentExtraction,
    PowerOfAttorneyExtraction,
    SealCertificateExtraction,
    ShareholderRegistryExtraction,
)


@dataclass(frozen=True)
class AssessmentStrategy:
    name: str = "deterministic_core"
    max_parallel_documents: int = 4


class AssessmentService:
    OWNERSHIP_SOURCE_TYPES = {
        DocumentType.SHAREHOLDER_REGISTRY,
        DocumentType.STOCK_CHANGE_STATEMENT,
        DocumentType.INVESTOR_REGISTRY,
        DocumentType.MEMBER_REGISTRY,
        DocumentType.BENEFICIAL_OWNER_PROOF_DOCUMENT,
        DocumentType.ARTICLES_OF_ASSOCIATION,
        DocumentType.OPERATING_RULES,
        DocumentType.REGULATIONS,
        DocumentType.MEETING_MINUTES,
        DocumentType.PURPOSE_PROOF_DOCUMENT,
    }

    def __init__(
        self,
        *,
        extraction_provider: DocumentExtractionProvider | None = None,
        strategy: AssessmentStrategy | None = None,
        ownership_threshold_percent: float = 25.0,
        ownership_total_tolerance_percent: float = 1.0,
    ) -> None:
        self.extraction_provider = extraction_provider
        self.strategy = strategy or AssessmentStrategy()
        self.extractor = StructuredPayloadExtractor()
        self.required_documents = RequiredDocumentsEngine()
        self.consistency_checker = CrossDocumentConsistencyChecker()
        self.beneficial_owner_engine = BeneficialOwnerEngine(
            threshold_percent=ownership_threshold_percent,
            total_tolerance_percent=ownership_total_tolerance_percent,
        )
        self.delegation_engine = DelegationAuthorityEngine()
        self.decision_engine = DecisionEngine()

    def assess(self, application: KycApplication, documents: list[DocumentMetadata]) -> KycAssessment:
        analyzed_documents = [self.extraction_provider.extract(document) if self.extraction_provider else document for document in documents]
        document_results: list[DocumentResult] = []
        extracted_by_type: dict[DocumentType, Any] = {}
        extracted_items: list[tuple[DocumentType, Any]] = []

        for document in analyzed_documents:
            predicted = document.predictedDocumentType or document.declaredDocumentType or DocumentType.UNKNOWN
            extracted = self.extractor.extract(document)
            if extracted is not None:
                extracted_by_type.setdefault(predicted, extracted)
                extracted_items.append((predicted, extracted))
            document_results.append(
                DocumentResult(
                    documentId=document.documentId,
                    declaredDocumentType=document.declaredDocumentType,
                    predictedDocumentType=predicted,
                    classificationConfidence=document.classificationConfidence or 1.0,
                    classificationReason="structured payload" if extracted is not None else "metadata only",
                    extracted=extracted.model_dump(mode="json") if extracted is not None else {},
                )
            )

        present_types = {result.predictedDocumentType for result in document_results if result.predictedDocumentType}
        purpose_verified_types = self._purpose_verified_types(extracted_items)
        missing_documents = self.required_documents.check(
            application.legalEntityType,
            application.applicantRole,
            present_types,
            purpose_verified_types=purpose_verified_types,
            is_nonprofit=application.isNonprofit,
        )
        extracted_by_name = {document_type.value: value for document_type, value in extracted_by_type.items()}
        checks = self.consistency_checker.check(
            extracted_by_name,
            {
                "businessRegistrationNumber": application.businessRegistrationNumber,
                "corporateRegistrationNumber": application.corporateRegistrationNumber,
            },
        )
        br = self._typed(extracted_by_type, DocumentType.BUSINESS_REGISTRATION, BusinessRegistrationExtraction)
        cr = self._typed(extracted_by_type, DocumentType.CORPORATE_REGISTRY, CorporateRegistryExtraction)
        poa = self._typed(extracted_by_type, DocumentType.POWER_OF_ATTORNEY, PowerOfAttorneyExtraction)
        seal_certificate = self._typed(extracted_by_type, DocumentType.SEAL_CERTIFICATE, SealCertificateExtraction)
        representative_name = self._representative_name(br, cr, extracted_by_type)
        corporate_name = self._corporate_name(br, cr, extracted_by_type)
        ownership_registries = self._ownership_registries(extracted_items)
        shareholder_registry = self._primary_ownership_registry(ownership_registries, corporate_name)
        beneficial_ownership = self.beneficial_owner_engine.evaluate_recursive(
            shareholder_registry,
            ownership_registries,
            representative_name,
        )
        representative_profile = self._representative_profile(br, cr, extracted_by_type)
        checks.extend(self._declared_input_checks(application, representative_profile, beneficial_ownership.owners))
        checks.extend(self._seal_consistency_checks(extracted_items))

        delegation = None
        if application.applicantRole == ApplicantRole.DELEGATE:
            delegation = self.delegation_engine.evaluate(
                poa,
                representative_name,
                application.applicantName,
                corporate_name,
                seal_certificate=seal_certificate,
            )

        status, supplement_requests, manual_review_reasons = self.decision_engine.decide(
            missing_documents=missing_documents,
            checks=checks,
            beneficial_ownership=beneficial_ownership,
            delegation=delegation,
            applicant_role=application.applicantRole,
        )
        return KycAssessment(
            assessmentId=f"ai_assess_{application.kycApplicationId}",
            kycApplicationId=application.kycApplicationId,
            legalEntityType=application.legalEntityType,
            applicantRole=application.applicantRole,
            status=status,
            overallConfidence=self._overall_confidence(document_results, checks, beneficial_ownership.owners),
            summary=self._summary(status, supplement_requests, manual_review_reasons),
            documentResults=document_results,
            extractedFields=self._extracted_fields(
                br,
                cr,
                shareholder_registry,
                beneficial_ownership.owners,
                extracted_items,
                representative_profile,
                application,
            ),
            crossDocumentChecks=checks,
            beneficialOwnership=beneficial_ownership,
            delegation=delegation,
            supplementRequests=supplement_requests,
            manualReviewReasons=manual_review_reasons,
            providerUsageLogs=[],
            modelMetadata=ModelMetadata(),
        )

    def _typed(self, extracted_by_type: dict[DocumentType, Any], document_type: DocumentType, model):
        value = extracted_by_type.get(document_type)
        return value if isinstance(value, model) else None

    def _purpose_verified_types(self, extracted_items: Iterable[tuple[DocumentType, Any]]) -> set[DocumentType]:
        verified = set()
        for document_type, extracted in extracted_items:
            purpose = getattr(extracted, "purposeVerification", None)
            if purpose and purpose.purposeVerificationSatisfied:
                verified.add(document_type)
        return verified

    def _representative_name(
        self,
        br: BusinessRegistrationExtraction | None,
        cr: CorporateRegistryExtraction | None,
        extracted_by_type: dict[DocumentType, Any],
    ) -> str | None:
        for value in [
            getattr(getattr(br, "representativeName", None), "normalized", None),
            getattr(getattr(cr, "representativeName", None), "normalized", None),
            getattr(getattr(getattr(br, "representative", None), "name", None), "normalized", None),
            getattr(getattr(getattr(cr, "representative", None), "name", None), "normalized", None),
        ]:
            if value:
                return str(value)
        for extracted in extracted_by_type.values():
            representative = getattr(extracted, "representative", None)
            value = getattr(getattr(representative, "name", None), "normalized", None)
            if value:
                return str(value)
        return None

    def _corporate_name(
        self,
        br: BusinessRegistrationExtraction | None,
        cr: CorporateRegistryExtraction | None,
        extracted_by_type: dict[DocumentType, Any],
    ) -> str | None:
        for value in [
            getattr(getattr(br, "legalName", None), "normalized", None),
            getattr(getattr(cr, "legalName", None), "normalized", None),
        ]:
            if value:
                return str(value)
        for extracted in extracted_by_type.values():
            value = getattr(getattr(extracted, "legalName", None), "normalized", None)
            if value:
                return str(value)
        return None

    def _ownership_registries(self, extracted_items: Iterable[tuple[DocumentType, Any]]) -> list[ShareholderRegistryExtraction]:
        registries = []
        for document_type, extracted in extracted_items:
            if document_type in self.OWNERSHIP_SOURCE_TYPES and isinstance(extracted, ShareholderRegistryExtraction):
                registries.append(extracted)
            if isinstance(extracted, OrganizationDocumentExtraction) and extracted.beneficialOwners:
                registries.append(
                    ShareholderRegistryExtraction(
                        legalName=extracted.legalName,
                        shareholders=extracted.beneficialOwners,
                        sealImpressionId=extracted.sealImpressionId,
                    )
                )
        return registries

    def _primary_ownership_registry(
        self,
        registries: list[ShareholderRegistryExtraction],
        corporate_name: str | None,
    ) -> ShareholderRegistryExtraction | None:
        if not registries:
            return None
        if corporate_name:
            for registry in registries:
                if registry.legalName.normalized == corporate_name or registry.legalName.raw == corporate_name:
                    return registry
        return registries[0]

    def _representative_profile(
        self,
        br: BusinessRegistrationExtraction | None,
        cr: CorporateRegistryExtraction | None,
        extracted_by_type: dict[DocumentType, Any],
    ) -> dict[str, Any]:
        for person in [getattr(br, "representative", None), getattr(cr, "representative", None)]:
            if person and person.name.normalized:
                return person.model_dump(mode="json")
        name = self._representative_name(br, cr, extracted_by_type)
        return {"name": {"raw": name, "normalized": name}} if name else {}

    def _declared_input_checks(
        self,
        application: KycApplication,
        representative_profile: dict[str, Any],
        owners: list[BeneficialOwner],
    ) -> list[CrossDocumentCheck]:
        checks = []
        declared = application.declaredRepresentative
        representative_name = _field_value(representative_profile.get("name"))
        if declared and declared.name and representative_name:
            checks.append(
                self._value_check(
                    "DECLARED_REPRESENTATIVE_NAME_MATCH",
                    declared.name,
                    representative_name,
                    "Declared representative matches extracted representative.",
                    "Declared representative differs from extracted representative.",
                )
            )
        if application.declaredBeneficialOwners:
            checks.append(self._declared_owner_check(application.declaredBeneficialOwners, owners))
        return checks

    def _declared_owner_check(
        self,
        declared_owners: list[DeclaredBeneficialOwner],
        owners: list[BeneficialOwner],
    ) -> CrossDocumentCheck:
        declared_names = {self._name_key(owner.name) for owner in declared_owners if owner.name}
        resolved_names = {self._name_key(owner.name) for owner in owners if owner.name}
        passed = bool(declared_names) and declared_names <= resolved_names
        return CrossDocumentCheck(
            checkCode="DECLARED_BENEFICIAL_OWNER_MATCH",
            status=CheckStatus.PASS if passed else CheckStatus.FAIL,
            severity=Severity.LOW if passed else Severity.HIGH,
            message="Declared beneficial owners match resolved owners." if passed else "Declared beneficial owners differ from resolved owners.",
            values=[
                {"source": "declared", "normalized": sorted(declared_names)},
                {"source": "resolved", "normalized": sorted(resolved_names)},
            ],
        )

    def _value_check(self, code: str, expected: str, actual: str, pass_message: str, fail_message: str) -> CrossDocumentCheck:
        passed = self._name_key(expected) == self._name_key(actual)
        return CrossDocumentCheck(
            checkCode=code,
            status=CheckStatus.PASS if passed else CheckStatus.FAIL,
            severity=Severity.LOW if passed else Severity.HIGH,
            message=pass_message if passed else fail_message,
            values=[{"source": "declared", "normalized": expected}, {"source": "extracted", "normalized": actual}],
        )

    def _seal_consistency_checks(self, extracted_items: Iterable[tuple[DocumentType, Any]]) -> list[CrossDocumentCheck]:
        by_entity: dict[str, list[dict[str, Any]]] = {}
        for document_type, extracted in extracted_items:
            seal = getattr(getattr(extracted, "sealImpressionId", None), "normalized", None)
            if not seal:
                continue
            entity = (
                getattr(getattr(extracted, "legalName", None), "normalized", None)
                or getattr(getattr(extracted, "corporateName", None), "normalized", None)
                or getattr(getattr(extracted, "targetCorporateName", None), "normalized", None)
            )
            if not entity:
                continue
            by_entity.setdefault(str(entity), []).append({"documentType": document_type.value, "sealImpressionId": str(seal)})

        checks = []
        for entity, values in by_entity.items():
            unique_seals = {item["sealImpressionId"] for item in values}
            if len(values) < 2:
                continue
            checks.append(
                CrossDocumentCheck(
                    checkCode="SAME_ENTITY_SEAL_MATCH",
                    status=CheckStatus.PASS if len(unique_seals) == 1 else CheckStatus.FAIL,
                    severity=Severity.LOW if len(unique_seals) == 1 else Severity.HIGH,
                    message="Same-entity seal impressions are consistent."
                    if len(unique_seals) == 1
                    else "Same-entity seal impressions differ across submitted documents.",
                    values=[{"entity": entity, **item} for item in values],
                )
            )
        return checks

    def _extracted_fields(
        self,
        br: BusinessRegistrationExtraction | None,
        cr: CorporateRegistryExtraction | None,
        shareholder_registry: ShareholderRegistryExtraction | None,
        owners: list[BeneficialOwner],
        extracted_items: Iterable[tuple[DocumentType, Any]],
        representative_profile: dict[str, Any],
        application: KycApplication,
    ) -> dict[str, Any]:
        purpose_documents = []
        for document_type, extracted in extracted_items:
            purpose = getattr(extracted, "purposeVerification", None)
            if purpose:
                purpose_documents.append({"documentType": document_type.value, **purpose.model_dump(mode="json")})
        legal_name = getattr(getattr(br, "legalName", None), "normalized", None) or getattr(getattr(cr, "legalName", None), "normalized", None)
        return {
            "corporateProfile": {
                "legalName": _value_payload(getattr(br, "legalName", None) or getattr(cr, "legalName", None)),
                "businessRegistrationNumber": _value_payload(getattr(br, "businessRegistrationNumber", None)),
                "corporateRegistrationNumber": _value_payload(getattr(cr, "corporateRegistrationNumber", None)),
                "representative": representative_profile,
                "nonProfit": application.isNonprofit,
            },
            "shareholderRegistry": shareholder_registry.model_dump(mode="json") if shareholder_registry else None,
            "beneficialOwners": [owner.model_dump(mode="json") for owner in owners],
            "purposeVerification": {
                "satisfied": any(doc.get("purposeVerificationSatisfied") for doc in purpose_documents),
                "documents": purpose_documents,
            },
            "legalName": legal_name,
        }

    def _overall_confidence(
        self,
        document_results: list[DocumentResult],
        checks: list[CrossDocumentCheck],
        owners: list[BeneficialOwner],
    ) -> float:
        values = [result.classificationConfidence for result in document_results]
        values.extend(check.confidence for check in checks if check.status == CheckStatus.PASS)
        values.extend(owner.confidence for owner in owners)
        return round(sum(values) / len(values), 4) if values else 0.0

    def _summary(self, status, supplement_requests: list[EngineIssue], manual_review_reasons: list[EngineIssue]) -> str:
        if supplement_requests:
            return f"{status}: {len(supplement_requests)} supplement issue(s), {len(manual_review_reasons)} review issue(s)."
        if manual_review_reasons:
            return f"{status}: {len(manual_review_reasons)} manual review issue(s)."
        return f"{status}: deterministic checks passed."

    def _name_key(self, value: str | None) -> str:
        return "".join(str(value or "").upper().split())


def _field_value(value: Any) -> Any:
    if isinstance(value, dict):
        normalized = value.get("normalized")
        return normalized if normalized not in (None, "") else value.get("raw")
    return value


def _value_payload(value: Any) -> Any:
    if hasattr(value, "model_dump"):
        return value.model_dump(mode="json")
    return value
