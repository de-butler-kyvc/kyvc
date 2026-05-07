from dataclasses import dataclass

from app.ai_assessment.enums import ApplicantRole, DocumentType, LegalEntityType
from app.ai_assessment.schemas import EngineIssue


@dataclass(frozen=True)
class AlternativeDocumentGroup:
    code: str
    options: frozenset[DocumentType]
    message: str


@dataclass(frozen=True)
class DocumentRequirementRule:
    mandatory: frozenset[DocumentType] = frozenset()
    alternatives: tuple[AlternativeDocumentGroup, ...] = ()
    purpose_verification_required: bool = False
    purpose_acceptable_types: frozenset[DocumentType] = frozenset()
    purpose_invalid_types: frozenset[DocumentType] = frozenset()
    nonprofit_support_alternatives: tuple[AlternativeDocumentGroup, ...] = ()


PURPOSE_POLICY_INVALID_TYPES = frozenset(
    {
        DocumentType.ORGANIZATION_IDENTITY_CERTIFICATE,
        DocumentType.OFFICIAL_LETTER,
        DocumentType.MEETING_MINUTES,
    }
)

GENERAL_PURPOSE_ACCEPTABLE_TYPES = frozenset(
    {
        DocumentType.ARTICLES_OF_ASSOCIATION,
        DocumentType.OPERATING_RULES,
        DocumentType.REGULATIONS,
        DocumentType.PURPOSE_PROOF_DOCUMENT,
        DocumentType.INVESTMENT_REGISTRATION_CERTIFICATE,
    }
)


class RequiredDocumentsEngine:
    STOCK_COMPANY_OWNER_DOCS = frozenset({DocumentType.SHAREHOLDER_REGISTRY, DocumentType.STOCK_CHANGE_STATEMENT})
    PARTNERSHIP_OWNER_DOCS = frozenset(
        {DocumentType.INVESTOR_REGISTRY, DocumentType.MEMBER_REGISTRY, DocumentType.ARTICLES_OF_ASSOCIATION}
    )
    ASSOCIATION_STRUCTURE_DOCS = frozenset(
        {
            DocumentType.INVESTOR_REGISTRY,
            DocumentType.MEMBER_REGISTRY,
            DocumentType.BOARD_REGISTRY,
            DocumentType.ARTICLES_OF_ASSOCIATION,
        }
    )
    COOPERATIVE_STRUCTURE_DOCS = frozenset(
        {
            DocumentType.ARTICLES_OF_ASSOCIATION,
            DocumentType.OPERATING_RULES,
            DocumentType.REGULATIONS,
            DocumentType.INVESTOR_REGISTRY,
            DocumentType.MEMBER_REGISTRY,
            DocumentType.MEETING_MINUTES,
        }
    )
    UNIQUE_NUMBER_SUPPORT_DOCS = frozenset(
        {
            DocumentType.ARTICLES_OF_ASSOCIATION,
            DocumentType.OPERATING_RULES,
            DocumentType.REGULATIONS,
            DocumentType.MEETING_MINUTES,
            DocumentType.OFFICIAL_LETTER,
        }
    )
    FOREIGN_OWNER_DOCS = frozenset({DocumentType.SHAREHOLDER_REGISTRY, DocumentType.BENEFICIAL_OWNER_PROOF_DOCUMENT})
    FOREIGN_NONPROFIT_SUPPORT_DOCS = frozenset(
        {
            DocumentType.ARTICLES_OF_ASSOCIATION,
            DocumentType.OPERATING_RULES,
            DocumentType.REGULATIONS,
            DocumentType.MEETING_MINUTES,
        }
    )

    RULES: dict[LegalEntityType, DocumentRequirementRule] = {
        LegalEntityType.STOCK_COMPANY: DocumentRequirementRule(
            mandatory=frozenset({DocumentType.BUSINESS_REGISTRATION, DocumentType.CORPORATE_REGISTRY}),
            alternatives=(
                AlternativeDocumentGroup(
                    code="SHAREHOLDER_REGISTRY_OR_STOCK_CHANGE_STATEMENT",
                    options=STOCK_COMPANY_OWNER_DOCS,
                    message="Stock company requires shareholder registry or stock change statement.",
                ),
            ),
        ),
        LegalEntityType.LIMITED_COMPANY: DocumentRequirementRule(
            mandatory=frozenset({DocumentType.BUSINESS_REGISTRATION, DocumentType.CORPORATE_REGISTRY}),
            alternatives=(
                AlternativeDocumentGroup(
                    code="PARTNERSHIP_OWNERSHIP_DOCUMENT",
                    options=PARTNERSHIP_OWNER_DOCS,
                    message="Limited company requires investor registry, member registry, or articles of association.",
                ),
            ),
        ),
        LegalEntityType.LIMITED_PARTNERSHIP: DocumentRequirementRule(
            mandatory=frozenset({DocumentType.BUSINESS_REGISTRATION, DocumentType.CORPORATE_REGISTRY}),
            alternatives=(
                AlternativeDocumentGroup(
                    code="PARTNERSHIP_OWNERSHIP_DOCUMENT",
                    options=PARTNERSHIP_OWNER_DOCS,
                    message="Limited partnership requires investor registry, member registry, or articles of association.",
                ),
            ),
        ),
        LegalEntityType.GENERAL_PARTNERSHIP: DocumentRequirementRule(
            mandatory=frozenset({DocumentType.BUSINESS_REGISTRATION, DocumentType.CORPORATE_REGISTRY}),
            alternatives=(
                AlternativeDocumentGroup(
                    code="PARTNERSHIP_OWNERSHIP_DOCUMENT",
                    options=PARTNERSHIP_OWNER_DOCS,
                    message="General partnership requires investor registry, member registry, or articles of association.",
                ),
            ),
        ),
        LegalEntityType.INCORPORATED_ASSOCIATION: DocumentRequirementRule(
            mandatory=frozenset({DocumentType.BUSINESS_REGISTRATION, DocumentType.CORPORATE_REGISTRY}),
            alternatives=(
                AlternativeDocumentGroup(
                    code="ASSOCIATION_STRUCTURE_DOCUMENT",
                    options=ASSOCIATION_STRUCTURE_DOCS,
                    message="Association requires investor registry, member registry, board registry, or articles.",
                ),
            ),
            purpose_verification_required=True,
            purpose_acceptable_types=GENERAL_PURPOSE_ACCEPTABLE_TYPES,
            purpose_invalid_types=PURPOSE_POLICY_INVALID_TYPES,
        ),
        LegalEntityType.FOUNDATION: DocumentRequirementRule(
            mandatory=frozenset({DocumentType.BUSINESS_REGISTRATION, DocumentType.CORPORATE_REGISTRY}),
            alternatives=(
                AlternativeDocumentGroup(
                    code="ASSOCIATION_STRUCTURE_DOCUMENT",
                    options=ASSOCIATION_STRUCTURE_DOCS,
                    message="Foundation requires investor registry, member registry, board registry, or articles.",
                ),
            ),
            purpose_verification_required=True,
            purpose_acceptable_types=GENERAL_PURPOSE_ACCEPTABLE_TYPES,
            purpose_invalid_types=PURPOSE_POLICY_INVALID_TYPES,
        ),
        LegalEntityType.COOPERATIVE: DocumentRequirementRule(
            mandatory=frozenset({DocumentType.BUSINESS_REGISTRATION}),
            alternatives=(
                AlternativeDocumentGroup(
                    code="COOPERATIVE_IDENTITY_DOCUMENT",
                    options=frozenset(
                        {
                            DocumentType.CORPORATE_REGISTRY,
                            DocumentType.ARTICLES_OF_ASSOCIATION,
                            DocumentType.OPERATING_RULES,
                            DocumentType.REGULATIONS,
                        }
                    ),
                    message="Cooperative requires corporate registry or articles/bylaws/regulations.",
                ),
                AlternativeDocumentGroup(
                    code="COOPERATIVE_STRUCTURE_DOCUMENT",
                    options=COOPERATIVE_STRUCTURE_DOCS,
                    message="Cooperative requires articles, rules, regulations, investor/member registry, or meeting minutes.",
                ),
            ),
            purpose_verification_required=True,
            purpose_acceptable_types=GENERAL_PURPOSE_ACCEPTABLE_TYPES,
            purpose_invalid_types=PURPOSE_POLICY_INVALID_TYPES,
        ),
        LegalEntityType.UNIQUE_NUMBER_ORGANIZATION: DocumentRequirementRule(
            mandatory=frozenset({DocumentType.ORGANIZATION_IDENTITY_CERTIFICATE}),
            alternatives=(
                AlternativeDocumentGroup(
                    code="UNIQUE_NUMBER_SUPPORT_DOCUMENT",
                    options=UNIQUE_NUMBER_SUPPORT_DOCS,
                    message="Unique-number organization requires articles, rules, regulations, meeting minutes, or official letter.",
                ),
            ),
            purpose_verification_required=True,
            purpose_acceptable_types=GENERAL_PURPOSE_ACCEPTABLE_TYPES,
            purpose_invalid_types=PURPOSE_POLICY_INVALID_TYPES,
        ),
        LegalEntityType.FOREIGN_COMPANY: DocumentRequirementRule(
            mandatory=frozenset({DocumentType.INVESTMENT_REGISTRATION_CERTIFICATE, DocumentType.CORPORATE_REGISTRY}),
            alternatives=(
                AlternativeDocumentGroup(
                    code="FOREIGN_BENEFICIAL_OWNER_DOCUMENT",
                    options=FOREIGN_OWNER_DOCS,
                    message="Foreign company requires shareholder registry or beneficial-owner proof document.",
                ),
            ),
            nonprofit_support_alternatives=(
                AlternativeDocumentGroup(
                    code="FOREIGN_NONPROFIT_SUPPORT_DOCUMENT",
                    options=FOREIGN_NONPROFIT_SUPPORT_DOCS,
                    message="Foreign nonprofit requires articles, rules, regulations, or meeting minutes.",
                ),
            ),
            purpose_verification_required=True,
            purpose_acceptable_types=GENERAL_PURPOSE_ACCEPTABLE_TYPES,
            purpose_invalid_types=PURPOSE_POLICY_INVALID_TYPES,
        ),
    }

    def check(
        self,
        legal_entity_type: LegalEntityType,
        applicant_role: ApplicantRole,
        present_types: set[DocumentType],
        *,
        purpose_verified_types: set[DocumentType] | None = None,
        is_nonprofit: bool = False,
    ) -> list[EngineIssue]:
        purpose_verified_types = purpose_verified_types or set()
        rule = self.RULES.get(legal_entity_type, DocumentRequirementRule())
        issues: list[EngineIssue] = []

        for document_type in sorted(rule.mandatory - present_types):
            issues.append(
                EngineIssue(
                    code=f"REQUIRED_DOCUMENT_MISSING_{document_type}",
                    message=f"Required document missing: {document_type}",
                )
            )

        for group in rule.alternatives:
            if not present_types & group.options:
                issues.append(EngineIssue(code=f"REQUIRED_DOCUMENT_GROUP_MISSING_{group.code}", message=group.message))

        if is_nonprofit:
            for group in rule.nonprofit_support_alternatives:
                if not present_types & group.options:
                    issues.append(EngineIssue(code=f"REQUIRED_DOCUMENT_GROUP_MISSING_{group.code}", message=group.message))

        if rule.purpose_verification_required and not purpose_verified_types & rule.purpose_acceptable_types:
            blocking_types = sorted((present_types | purpose_verified_types) & rule.purpose_invalid_types)
            detail = f" Present document types cannot prove purpose by policy: {', '.join(blocking_types)}." if blocking_types else ""
            issues.append(
                EngineIssue(
                    code="PURPOSE_VERIFICATION_DOCUMENT_MISSING",
                    message="Establishment purpose must be verified by an acceptable document class." + detail,
                )
            )

        if applicant_role == ApplicantRole.DELEGATE and DocumentType.POWER_OF_ATTORNEY not in present_types:
            issues.append(
                EngineIssue(
                    code=f"REQUIRED_DOCUMENT_MISSING_{DocumentType.POWER_OF_ATTORNEY}",
                    message=f"Required document missing: {DocumentType.POWER_OF_ATTORNEY}",
                )
            )
        if applicant_role == ApplicantRole.DELEGATE and DocumentType.SEAL_CERTIFICATE not in present_types:
            issues.append(
                EngineIssue(
                    code=f"REQUIRED_DOCUMENT_MISSING_{DocumentType.SEAL_CERTIFICATE}",
                    message=f"Required document missing: {DocumentType.SEAL_CERTIFICATE}",
                )
            )
        return issues
