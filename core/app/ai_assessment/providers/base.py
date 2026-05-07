from typing import Protocol

from app.ai_assessment.schemas import DocumentMetadata


class DocumentExtractionProvider(Protocol):
    """Provider boundary for OCR/LLM assisted extraction.

    Providers may enrich document metadata with structured extracted payloads,
    but final assessment status is still produced by deterministic engines.
    """

    provider_name: str

    def extract(self, document: DocumentMetadata) -> DocumentMetadata:
        ...
