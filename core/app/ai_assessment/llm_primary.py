import base64
import hashlib
import mimetypes
from pathlib import Path
from uuid import uuid4

from app.ai_assessment.api_models import LlmPrimaryAssessmentRequest, LlmPrimaryAssessmentResponse
from app.ai_assessment.providers.factory import build_document_extraction_provider
from app.ai_assessment.schemas import DocumentMetadata, KycApplication
from app.ai_assessment.service import AssessmentService
from app.core.config import Settings


def assess_documents_with_llm_primary(
    payload: LlmPrimaryAssessmentRequest,
    settings: Settings,
) -> LlmPrimaryAssessmentResponse:
    provider = build_document_extraction_provider(settings)
    if provider is None:
        raise ValueError("LLM_PROVIDER must be configured for llm_primary assessment.")
    if not payload.documents:
        raise ValueError("At least one document is required for llm_primary assessment.")

    application = KycApplication(
        kycApplicationId=payload.kycApplicationId,
        legalEntityType=payload.legalEntityType,
        applicantRole=payload.applicantRole,
        applicantName=payload.applicantName,
        isNonprofit=payload.isNonprofit,
        businessRegistrationNumber=payload.businessRegistrationNumber,
        corporateRegistrationNumber=payload.corporateRegistrationNumber,
        declaredRepresentative=payload.declaredRepresentative,
        declaredBeneficialOwners=payload.declaredBeneficialOwners,
    )
    documents = [
        _document_metadata(document, payload.kycApplicationId, settings.app_storage_path)
        for document in payload.documents
    ]
    assessment = AssessmentService(
        extraction_provider=provider,
        ownership_threshold_percent=settings.ownership_threshold_percent,
        ownership_total_tolerance_percent=settings.ownership_total_tolerance_percent,
    ).assess(application, documents)
    return LlmPrimaryAssessmentResponse(
        extractionProvider=provider.provider_name,
        assessment=assessment,
        documents=documents,
    )


def _document_metadata(document, kyc_application_id: str, storage_root: str) -> DocumentMetadata:
    document_id = document.documentId or f"doc_{uuid4().hex}"
    storage_path = document.storagePath
    materialized = None
    if document.contentBase64 is not None:
        materialized = base64.b64decode(document.contentBase64, validate=True)
        storage_path = _write_document_bytes(storage_root, kyc_application_id, document_id, document.originalFileName, materialized)
    elif document.textContent is not None:
        materialized = document.textContent.encode("utf-8")
        storage_path = _write_document_bytes(storage_root, kyc_application_id, document_id, document.originalFileName, materialized)

    if materialized is None and storage_path:
        path = Path(storage_path)
        if path.exists():
            materialized = path.read_bytes()

    return DocumentMetadata(
        documentId=document_id,
        kycApplicationId=kyc_application_id,
        originalFileName=document.originalFileName,
        mimeType=document.mimeType or mimetypes.guess_type(document.originalFileName)[0] or "application/octet-stream",
        sizeBytes=document.sizeBytes or (len(materialized) if materialized is not None else 0),
        sha256=document.sha256 or (_sha256(materialized) if materialized is not None else None),
        declaredDocumentType=document.declaredDocumentType,
        predictedDocumentType=document.declaredDocumentType,
        storagePath=storage_path,
        extracted=document.extracted,
    )


def _write_document_bytes(storage_root: str, kyc_application_id: str, document_id: str, original_name: str, data: bytes) -> str:
    root = Path(storage_root).expanduser() / "ai-assessment" / kyc_application_id
    root.mkdir(parents=True, exist_ok=True)
    suffix = Path(original_name).suffix or ".bin"
    path = root / f"{document_id}{suffix}"
    path.write_bytes(data)
    return str(path)


def _sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()
