from __future__ import annotations

from typing import Any, Literal, Optional

from pydantic import BaseModel, Field

ProviderCategory = Literal["ocr", "llm"]


class ProviderSelectionUpdateRequest(BaseModel):
    provider: str = Field(min_length=1)
    profile: str = Field(default="default", min_length=1)
    changed_by: Optional[str] = None


class CoreProxyResponse(BaseModel):
    data: dict[str, Any]
