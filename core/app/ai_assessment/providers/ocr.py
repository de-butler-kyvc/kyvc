import base64
import mimetypes
import time
import uuid
from pathlib import Path
from typing import Protocol

import httpx

from app.ai_assessment.schemas import DocumentMetadata
from app.resilience.outbound import execute_outbound


class OcrTextProvider(Protocol):
    provider_name: str

    def extract_text(self, document: DocumentMetadata) -> str:
        ...


class AzureDocumentIntelligenceOcrTextProvider:
    provider_name = "azure_document_intelligence"

    def __init__(
        self,
        *,
        endpoint: str,
        key: str,
        model_id: str = "prebuilt-layout",
        api_version: str | None = None,
        timeout: float = 120.0,
        poll_interval_seconds: float = 2.0,
    ) -> None:
        self.endpoint = endpoint.rstrip("/")
        self.key = key
        self.model_id = model_id
        self.api_version = api_version or "2024-11-30"
        self.timeout = timeout
        self.poll_interval_seconds = poll_interval_seconds

    def extract_text(self, document: DocumentMetadata) -> str:
        path = _document_path(document)
        content_type = document.mimeType or mimetypes.guess_type(path.name)[0] or "application/octet-stream"

        def analyze() -> httpx.Response:
            response = httpx.post(
                f"{self.endpoint}/documentintelligence/documentModels/{self.model_id}:analyze",
                params={"api-version": self.api_version},
                headers={
                    "Ocp-Apim-Subscription-Key": self.key,
                    "Content-Type": content_type,
                },
                content=path.read_bytes(),
                timeout=self.timeout,
            )
            response.raise_for_status()
            return response

        response = execute_outbound("ocr", "azure_document_intelligence_analyze", analyze)
        operation_location = response.headers.get("operation-location") or response.headers.get("Operation-Location")
        if operation_location:
            result = self._poll(operation_location, initial_delay_seconds=_retry_after_seconds(response.headers))
        else:
            result = response.json()
        return _azure_text(result)

    def _poll(self, operation_location: str, *, initial_delay_seconds: float | None = None) -> dict:
        deadline = time.monotonic() + self.timeout
        last_payload = {}
        next_delay_seconds = initial_delay_seconds
        while time.monotonic() < deadline:
            if next_delay_seconds and next_delay_seconds > 0:
                time.sleep(next_delay_seconds)

            def poll() -> httpx.Response:
                response = httpx.get(
                    operation_location,
                    headers={"Ocp-Apim-Subscription-Key": self.key},
                    timeout=self.timeout,
                )
                response.raise_for_status()
                return response

            response = execute_outbound("ocr", "azure_document_intelligence_poll", poll)
            payload = response.json()
            last_payload = payload
            status = str(payload.get("status") or "").lower()
            if status == "succeeded":
                return payload.get("analyzeResult") or payload
            if status in {"failed", "canceled", "cancelled"}:
                raise RuntimeError(f"Azure Document Intelligence analyze failed: {payload}")
            next_delay_seconds = _retry_after_seconds(response.headers) or self.poll_interval_seconds
        raise TimeoutError(f"Azure Document Intelligence analyze timed out: {last_payload}")


class NaverClovaOcrTextProvider:
    provider_name = "naver_clova_ocr"

    def __init__(
        self,
        *,
        endpoint: str,
        secret: str,
        template_id: str | None = None,
        timeout: float = 120.0,
    ) -> None:
        self.endpoint = endpoint
        self.secret = secret
        self.template_id = template_id
        self.timeout = timeout

    def extract_text(self, document: DocumentMetadata) -> str:
        path = _document_path(document)
        suffix = path.suffix.lower().lstrip(".") or "pdf"
        image = {
            "format": "jpg" if suffix == "jpeg" else suffix,
            "name": path.stem,
            "data": base64.b64encode(path.read_bytes()).decode("ascii"),
        }
        if self.template_id:
            image["templateIds"] = [self.template_id]

        def call_clova() -> httpx.Response:
            response = httpx.post(
                self.endpoint,
                headers={
                    "X-OCR-SECRET": self.secret,
                    "Content-Type": "application/json",
                },
                json={
                    "version": "V2",
                    "requestId": str(uuid.uuid4()),
                    "timestamp": int(time.time() * 1000),
                    "images": [image],
                },
                timeout=self.timeout,
            )
            response.raise_for_status()
            return response

        response = execute_outbound("ocr", "naver_clova_ocr", call_clova)
        return _clova_text(response.json())


def _document_path(document: DocumentMetadata) -> Path:
    if not document.storagePath:
        raise ValueError(f"storagePath is required for OCR provider: {document.documentId}")
    path = Path(document.storagePath)
    if not path.exists():
        raise FileNotFoundError(path)
    return path


def _azure_text(payload: dict) -> str:
    content = payload.get("content")
    if content:
        return str(content)
    lines = []
    for page in payload.get("pages", []) or []:
        for line in page.get("lines", []) or []:
            text = line.get("content")
            if text:
                lines.append(str(text))
    for paragraph in payload.get("paragraphs", []) or []:
        text = paragraph.get("content")
        if text:
            lines.append(str(text))
    return "\n".join(lines)


def _clova_text(payload: dict) -> str:
    lines = []
    for image in payload.get("images", []) or []:
        current_line = []
        for field in image.get("fields", []) or []:
            text = field.get("inferText") or field.get("name")
            if not text:
                continue
            current_line.append(str(text))
            if field.get("lineBreak") is True:
                lines.append(" ".join(current_line))
                current_line = []
        if current_line:
            lines.append(" ".join(current_line))
    return "\n".join(lines)


def _retry_after_seconds(headers: httpx.Headers) -> float | None:
    value = headers.get("retry-after") or headers.get("Retry-After")
    if value is None:
        return None
    try:
        return max(0.0, float(value.strip()))
    except (TypeError, ValueError):
        return None
