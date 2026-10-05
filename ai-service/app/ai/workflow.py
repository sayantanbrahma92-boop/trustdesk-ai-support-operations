import re

from app.safety.guardrails import inspect_customer_message
from app.ai.providers import AIProvider
from app.knowledge.retrieval import KnowledgeRetriever, RetrievedDocument
from app.ai.schemas import DraftRequest, DraftResponse, TriageRequest, TriageResponse


class AIWorkflow:
    def __init__(self, provider: AIProvider, retriever: KnowledgeRetriever):
        self._provider = provider
        self._retriever = retriever

    def triage(self, request: TriageRequest) -> TriageResponse:
        decision = inspect_customer_message(
            f"{request.subject}\n{request.body}"
        )
        if decision:
            return TriageResponse(
                category=decision.category,
                priority=decision.priority,
                escalation=True,
            )

        return self._provider.triage(request)

    def draft(self, request: DraftRequest) -> DraftResponse:
        decision = inspect_customer_message(
            f"{request.subject}\n{request.body}"
        )
        if decision:
            return self._escalation(decision.reason)

        query = f"{request.subject}\n{request.body}"
        retrieved = self._retriever.search(query, limit=request.limit)
        retrieved_ids = [document.document_id for document in retrieved]

        trusted_evidence = [
            document
            for document in retrieved
            if not document.is_untrusted
            and not self._is_unrelated_warranty_evidence(request, document)
        ]

        if not trusted_evidence:
            return self._escalation(
                "No usable policy evidence was retrieved.",
                retrieved_ids,
            )

        result = self._provider.draft(request, trusted_evidence)
        valid_ids = {document.document_id for document in trusted_evidence}
        citations = list(dict.fromkeys(result.citation_ids))

        if (
            not result.supported
            or not result.draft
            or not result.draft.strip()
            or not citations
            or any(citation not in valid_ids for citation in citations)
        ):
            return self._escalation(
                "The answer could not be validated against retrieved policy.",
                retrieved_ids,
            )

        return DraftResponse(
            status="draft",
            draft=result.draft.strip(),
            citation_ids=citations,
            retrieved_document_ids=retrieved_ids,
            escalation=False,
        )

    @staticmethod
    def _is_unrelated_warranty_evidence(
        request: DraftRequest,
        document: RetrievedDocument,
    ) -> bool:
        if document.document_id != "KB-WARRANTY-001":
            return False

        message = f"{request.subject}\n{request.body}"
        reports_arrival_damage = (
            re.search(
                r"\b(?:arriv\w*|deliver\w*|receiv\w*)\b",
                message,
                re.IGNORECASE,
            )
            and re.search(
                r"\b(?:damag\w*|crack\w*|broken|defect\w*)\b",
                message,
                re.IGNORECASE,
            )
        )
        return bool(reports_arrival_damage)

    @staticmethod
    def _escalation(
        reason: str,
        retrieved_document_ids: list[str] | None = None,
    ) -> DraftResponse:
        return DraftResponse(
            status="escalate",
            draft=None,
            citation_ids=[],
            retrieved_document_ids=retrieved_document_ids or [],
            escalation=True,
            reason=reason,
        )