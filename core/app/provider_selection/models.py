from typing import Literal

from pydantic import BaseModel, Field

ProviderCategory = Literal["ocr", "llm"]


class ProviderSelection(BaseModel):
    category: ProviderCategory
    provider: str
    profile: str = "default"
    active: bool = True
    updated_by: str | None = None
    updated_at: str | None = None


class ProviderSelectionOption(BaseModel):
    category: ProviderCategory
    provider: str
    profiles: list[str] = Field(default_factory=list)
    configured: bool
    detail: str


class ProviderSelectionsResponse(BaseModel):
    selections: dict[ProviderCategory, ProviderSelection]


class ProviderSelectionOptionsResponse(BaseModel):
    options: dict[ProviderCategory, list[ProviderSelectionOption]]


class ProviderSelectionUpdateRequest(BaseModel):
    provider: str
    profile: str = "default"
    changed_by: str | None = None


class ProviderSelectionHistoryEntry(BaseModel):
    category: ProviderCategory
    previous_provider: str | None = None
    previous_profile: str | None = None
    new_provider: str
    new_profile: str
    changed_by: str | None = None
    changed_at: str


class ProviderSelectionHistoryResponse(BaseModel):
    history: list[ProviderSelectionHistoryEntry]
