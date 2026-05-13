from typing import Protocol

from app.ai_assessment.schemas import DocumentMetadata


class LlmExtractionError(RuntimeError):
    def __init__(
        self,
        message: str,
        *,
        document: DocumentMetadata,
        source_text: str | None = None,
        original_error: Exception | None = None,
    ) -> None:
        super().__init__(message)
        self.document = document
        self.source_text = source_text
        self.original_error = original_error


class DocumentExtractionProvider(Protocol):
    """Provider boundary for OCR/LLM assisted extraction.

    Providers may enrich document metadata with structured extracted payloads,
    but final assessment status is still produced by deterministic engines.
    """

    provider_name: str

    def extract(self, document: DocumentMetadata) -> DocumentMetadata:
        ...
