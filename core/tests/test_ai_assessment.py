from datetime import UTC, datetime, timedelta

from app.ai_assessment import AssessmentService, build_core_kyc_claims
from app.ai_assessment.enums import ApplicantRole, AssessmentStatus, DocumentType, HolderType, LegalEntityType
from app.ai_assessment.schemas import DeclaredBeneficialOwner, DocumentMetadata, KycApplication
from app.credentials.crypto import decode_compact_jws, generate_private_key
from app.credentials.did import did_from_account
from app.issuer.service import IssuerService


def _value(value, confidence=0.99):
    return {"raw": value, "normalized": value, "confidence": confidence}


def _person(name="Kim Representative", birth_date="1980-01-01", nationality="KR"):
    return {
        "name": _value(name),
        "birthDate": _value(birth_date),
        "nationality": _value(nationality),
    }


def _doc(document_id, document_type, extracted, *, app_id="app-normal"):
    return DocumentMetadata(
        documentId=document_id,
        kycApplicationId=app_id,
        declaredDocumentType=document_type,
        predictedDocumentType=document_type,
        classificationConfidence=0.98,
        sizeBytes=128,
        sha256=f"sha-{document_id}",
        extracted=extracted,
    )


def _business(name="KYvC Labs", representative="Kim Representative", seal=None):
    payload = {
        "legalName": _value(name),
        "businessRegistrationNumber": _value("123-45-67890"),
        "representativeName": _value(representative),
        "representative": _person(representative),
        "businessAddress": _value("Seoul"),
    }
    if seal:
        payload["sealImpressionId"] = _value(seal)
    return payload


def _registry(name="KYvC Labs", representative="Kim Representative", seal=None):
    payload = {
        "legalName": _value(name),
        "corporateRegistrationNumber": _value("110111-1234567"),
        "representativeName": _value(representative),
        "representative": _person(representative),
        "headOfficeAddress": _value("Seoul"),
        "purpose": _value("software business"),
    }
    if seal:
        payload["sealImpressionId"] = _value(seal)
    return payload


def _shareholder_registry(name="KYvC Labs", shareholders=None, total=1000, seal=None):
    payload = {
        "legalName": _value(name),
        "totalShares": _value(total),
        "shareholders": shareholders
        or [
            {
                "name": "Owner One",
                "holderType": HolderType.INDIVIDUAL,
                "birthDate": "1979-02-03",
                "nationality": "KR",
                "shares": 600,
                "ownershipPercent": 60.0,
            },
            {
                "name": "Owner Two",
                "holderType": HolderType.INDIVIDUAL,
                "shares": 400,
                "ownershipPercent": 40.0,
            },
        ],
    }
    if seal:
        payload["sealImpressionId"] = _value(seal)
    return payload


def _normal_application(app_id="app-normal", **updates):
    data = {
        "kycApplicationId": app_id,
        "legalEntityType": LegalEntityType.STOCK_COMPANY,
        "applicantRole": ApplicantRole.REPRESENTATIVE,
        "businessRegistrationNumber": "1234567890",
        "corporateRegistrationNumber": "1101111234567",
    }
    data.update(updates)
    return KycApplication(**data)


def _normal_documents(app_id="app-normal"):
    return [
        _doc("business", DocumentType.BUSINESS_REGISTRATION, _business(), app_id=app_id),
        _doc("registry", DocumentType.CORPORATE_REGISTRY, _registry(), app_id=app_id),
        _doc("owners", DocumentType.SHAREHOLDER_REGISTRY, _shareholder_registry(), app_id=app_id),
    ]


def _assess(application, documents):
    return AssessmentService().assess(application, documents)


def _codes(issues):
    return {issue.code for issue in issues}


class RecordingExtractionProvider:
    provider_name = "recording_test_provider"

    def __init__(self, extracted_by_document_id):
        self.extracted_by_document_id = extracted_by_document_id
        self.calls = []

    def extract(self, document):
        self.calls.append(document.documentId)
        extracted = self.extracted_by_document_id.get(document.documentId)
        if extracted is None:
            return document
        return document.model_copy(update={"extracted": extracted})


def test_normal_stock_company_scanned_case():
    assessment = _assess(_normal_application(), _normal_documents())

    assert assessment.status == AssessmentStatus.NORMAL
    assert [owner.name for owner in assessment.beneficialOwnership.owners] == ["Owner One", "Owner Two"]
    assert not assessment.supplementRequests
    assert not assessment.manualReviewReasons


def test_assessment_service_invokes_extraction_provider():
    documents = [
        DocumentMetadata(
            documentId="business",
            kycApplicationId="app-provider",
            declaredDocumentType=DocumentType.BUSINESS_REGISTRATION,
            predictedDocumentType=DocumentType.BUSINESS_REGISTRATION,
            classificationConfidence=0.98,
            sizeBytes=128,
            sha256="sha-business",
        ),
        DocumentMetadata(
            documentId="registry",
            kycApplicationId="app-provider",
            declaredDocumentType=DocumentType.CORPORATE_REGISTRY,
            predictedDocumentType=DocumentType.CORPORATE_REGISTRY,
            classificationConfidence=0.98,
            sizeBytes=128,
            sha256="sha-registry",
        ),
        DocumentMetadata(
            documentId="owners",
            kycApplicationId="app-provider",
            declaredDocumentType=DocumentType.SHAREHOLDER_REGISTRY,
            predictedDocumentType=DocumentType.SHAREHOLDER_REGISTRY,
            classificationConfidence=0.98,
            sizeBytes=128,
            sha256="sha-owners",
        ),
    ]
    provider = RecordingExtractionProvider(
        {
            "business": _business(),
            "registry": _registry(),
            "owners": _shareholder_registry(),
        }
    )

    assessment = AssessmentService(extraction_provider=provider).assess(
        _normal_application("app-provider"),
        documents,
    )

    assert provider.calls == ["business", "registry", "owners"]
    assert assessment.status == AssessmentStatus.NORMAL
    assert assessment.documentResults[0].extracted["legalName"]["normalized"] == "KYvC Labs"


def test_declared_beneficial_owner_mismatch_requires_manual_review():
    application = _normal_application(
        declaredBeneficialOwners=[DeclaredBeneficialOwner(name="Someone Else", ownershipPercent=60.0)]
    )

    assessment = _assess(application, _normal_documents())

    assert assessment.status == AssessmentStatus.MANUAL_REVIEW_REQUIRED
    assert "DECLARED_BENEFICIAL_OWNER_MATCH" in _codes(assessment.manualReviewReasons)


def test_recursive_corporate_shareholder_ownership_resolves_with_lower_tier_registry():
    documents = [
        _doc("business", DocumentType.BUSINESS_REGISTRATION, _business(), app_id="app-recursive"),
        _doc("registry", DocumentType.CORPORATE_REGISTRY, _registry(), app_id="app-recursive"),
        _doc(
            "owners-top",
            DocumentType.SHAREHOLDER_REGISTRY,
            _shareholder_registry(
                shareholders=[
                    {"name": "Holding Co", "holderType": HolderType.CORPORATE, "ownershipPercent": 60.0},
                    {"name": "Minor Owner", "holderType": HolderType.INDIVIDUAL, "ownershipPercent": 40.0},
                ]
            ),
            app_id="app-recursive",
        ),
        _doc(
            "owners-lower",
            DocumentType.SHAREHOLDER_REGISTRY,
            _shareholder_registry(
                name="Holding Co",
                shareholders=[
                    {"name": "Nested Owner", "holderType": HolderType.INDIVIDUAL, "ownershipPercent": 60.0},
                    {"name": "Nested Minor", "holderType": HolderType.INDIVIDUAL, "ownershipPercent": 40.0},
                ],
            ),
            app_id="app-recursive",
        ),
    ]

    assessment = _assess(_normal_application("app-recursive"), documents)

    assert assessment.status == AssessmentStatus.NORMAL
    assert assessment.beneficialOwnership.owners[0].name == "Nested Owner"
    assert assessment.beneficialOwnership.owners[0].ownershipPercent == 36.0


def test_delegate_requires_poa_and_seal_certificate():
    application = _normal_application(applicantRole=ApplicantRole.DELEGATE, applicantName="Lee Delegate")

    assessment = _assess(application, _normal_documents())

    assert assessment.status == AssessmentStatus.SUPPLEMENT_REQUIRED
    assert f"REQUIRED_DOCUMENT_MISSING_{DocumentType.POWER_OF_ATTORNEY}" in _codes(assessment.supplementRequests)
    assert f"REQUIRED_DOCUMENT_MISSING_{DocumentType.SEAL_CERTIFICATE}" in _codes(assessment.supplementRequests)


def _poa(seal="seal-a"):
    return {
        "delegatorName": _value("Kim Representative"),
        "delegateName": _value("Lee Delegate"),
        "targetCorporateName": _value("KYvC Labs"),
        "authorityText": _value("KYC application, document submission, VC receipt"),
        "canApplyKyc": _value(True),
        "canSubmitDocuments": _value(True),
        "canReceiveVc": _value(True),
        "validUntil": _value("2999-12-31"),
        "hasSignatureOrSeal": _value(True),
        "sealImpressionId": _value(seal),
    }


def _seal_certificate(seal="seal-a", company="KYvC Labs"):
    return {
        "subjectName": _value("Kim Representative"),
        "corporateName": _value(company),
        "certificateNumber": _value("CERT-1"),
        "sealImpressionId": _value(seal),
    }


def test_poa_seal_mismatch_requires_manual_review():
    application = _normal_application(
        "app-delegate",
        applicantRole=ApplicantRole.DELEGATE,
        applicantName="Lee Delegate",
    )
    documents = [
        *_normal_documents("app-delegate"),
        _doc("poa", DocumentType.POWER_OF_ATTORNEY, _poa("seal-a"), app_id="app-delegate"),
        _doc("seal", DocumentType.SEAL_CERTIFICATE, _seal_certificate("seal-b"), app_id="app-delegate"),
    ]

    assessment = _assess(application, documents)

    assert assessment.status == AssessmentStatus.MANUAL_REVIEW_REQUIRED
    assert "POA_SEAL_CERTIFICATE_MISMATCH" in _codes(assessment.manualReviewReasons)


def test_same_entity_seal_mismatch_requires_manual_review():
    documents = [
        _doc("business", DocumentType.BUSINESS_REGISTRATION, _business(seal="seal-a"), app_id="app-seal"),
        _doc("registry", DocumentType.CORPORATE_REGISTRY, _registry(seal="seal-b"), app_id="app-seal"),
        _doc("owners", DocumentType.SHAREHOLDER_REGISTRY, _shareholder_registry(), app_id="app-seal"),
    ]

    assessment = _assess(_normal_application("app-seal"), documents)

    assert assessment.status == AssessmentStatus.MANUAL_REVIEW_REQUIRED
    assert "SAME_ENTITY_SEAL_MATCH" in _codes(assessment.manualReviewReasons)


def test_purpose_verification_failure_for_official_letter_only_case():
    application = KycApplication(
        kycApplicationId="app-purpose",
        legalEntityType=LegalEntityType.UNIQUE_NUMBER_ORGANIZATION,
        applicantRole=ApplicantRole.REPRESENTATIVE,
    )
    org_cert = {
        "legalName": _value("Purpose Org"),
        "representative": _person(),
    }
    official_letter = {
        "legalName": _value("Purpose Org"),
        "representative": _person(),
        "purposeVerification": {
            "establishmentPurpose": _value("community service"),
            "acceptableForPurposeVerification": False,
            "purposeVerificationSatisfied": True,
        },
    }
    documents = [
        _doc("org-cert", DocumentType.ORGANIZATION_IDENTITY_CERTIFICATE, org_cert, app_id="app-purpose"),
        _doc("letter", DocumentType.OFFICIAL_LETTER, official_letter, app_id="app-purpose"),
    ]

    assessment = _assess(application, documents)

    assert assessment.status == AssessmentStatus.SUPPLEMENT_REQUIRED
    assert "PURPOSE_VERIFICATION_DOCUMENT_MISSING" in _codes(assessment.supplementRequests)


def test_core_sdjwt_issuance_from_generated_ai_claims():
    application = _normal_application()
    documents = _normal_documents()
    assessment = _assess(application, documents)
    claims = build_core_kyc_claims(assessment, documents, assurance_level="STANDARD")
    issuer_key = generate_private_key()
    issuer = IssuerService("rIssuer", issuer_key)
    holder_did = did_from_account("rHolder")

    credential, status, paths = issuer.issue_kyc_sd_jwt(
        holder_account="rHolder",
        holder_did=holder_did,
        claims=claims,
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=datetime.now(tz=UTC) + timedelta(days=1),
        persist=False,
        persist_status=False,
    )

    assert assessment.status == AssessmentStatus.NORMAL
    assert claims["legalEntity"]["type"] == "STOCK_COMPANY"
    assert status["credentialType"]
    assert paths
    _, payload, _, _ = decode_compact_jws(credential.split("~", 1)[0])
    assert payload["jti"].startswith("urn:uuid:")
