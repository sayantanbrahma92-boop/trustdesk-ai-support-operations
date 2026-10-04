from enum import StrEnum
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, StrictBool


class TriageCategory(StrEnum):
    SHIPPING = "shipping"
    REFUND = "refund"
    WARRANTY = "warranty"
    BILLING = "billing"
    ACCOUNT_SECURITY = "account_security"
    GENERAL = "general"


class TriagePriority(StrEnum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    URGENT = "urgent"


class TriageRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    ticket_id: str = Field(min_length=1)
    subject: str = Field(min_length=1)
    body: str = Field(min_length=1)
    customer_tier: str = Field(min_length=1)
    customer_verified: StrictBool


class TriageResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    category: TriageCategory
    priority: TriagePriority
    escalation: StrictBool


class RetrievalRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    query: str = Field(min_length=1, max_length=2000)
    limit: int = Field(default=5, ge=1, le=10)


class RetrievedDocumentResponse(BaseModel):
    document_id: str
    text: str
    is_untrusted: StrictBool


class RetrievalResponse(BaseModel):
    documents: list[RetrievedDocumentResponse]


class DraftRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    ticket_id: str = Field(min_length=1)
    subject: str = Field(min_length=1)
    body: str = Field(min_length=1)
    customer_tier: str = Field(min_length=1)
    customer_verified: StrictBool
    limit: int = Field(default=5, ge=1, le=10)


class DraftProviderResult(BaseModel):
    """Structured provider output; not yet trusted as a validated draft."""

    model_config = ConfigDict(extra="forbid")

    supported: StrictBool
    draft: str | None = None
    citation_ids: list[str] = Field(default_factory=list)


class DraftResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    status: Literal["draft", "escalate"]
    draft: str | None = None
    citation_ids: list[str] = Field(default_factory=list)
    retrieved_document_ids: list[str] = Field(default_factory=list)
    escalation: StrictBool
    reason: str | None = None