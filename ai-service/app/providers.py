from typing import Protocol

from app.retrieval import RetrievedDocument
from app.schemas import DraftProviderResult, DraftRequest, TriageRequest, TriageResponse


class AIProvider(Protocol):
    def triage(self, request: TriageRequest) -> TriageResponse:
        ...

    def draft(
        self,
        request: DraftRequest,
        knowledge: list[RetrievedDocument],
    ) -> DraftProviderResult:
        ...


class DeterministicFakeAIProvider:
    """Predictable placeholder for local development and tests; not an AI model."""

    def triage(self, request: TriageRequest) -> TriageResponse:
        return TriageResponse(
            category="general",
            priority="low",
            escalation=False,
        )

    def draft(
        self,
        request: DraftRequest,
        knowledge: list[RetrievedDocument],
    ) -> DraftProviderResult:
        if not knowledge:
            return DraftProviderResult(supported=False)

        return DraftProviderResult(
            supported=True,
            draft="Please review the cited support policy. Our team can help with the next steps.",
            citation_ids=[knowledge[0].document_id],
        )


def get_ai_provider() -> AIProvider:
    from app.openrouter_provider import OpenRouterAIProvider

    return OpenRouterAIProvider.from_environment()