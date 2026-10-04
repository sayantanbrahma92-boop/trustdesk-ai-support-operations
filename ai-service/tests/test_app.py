import pytest
from fastapi.testclient import TestClient
from pathlib import Path
from app.providers import get_ai_provider
from app.retrieval import RetrievedDocument
from app.schemas import (
    RetrievalRequest,
    TriageRequest,
    TriageResponse,
)
from app.main import app, get_knowledge_retriever
from app.embeddings import LocalSemanticEmbedder
from app.kb_loader import load_and_index_markdown_documents
from app.retrieval import KnowledgeRetriever

client = TestClient(app)


class RecordingTriageProvider:
    def __init__(self):
        self.calls: list[TriageRequest] = []

    def triage(self, request: TriageRequest) -> TriageResponse:
        self.calls.append(request)
        return TriageResponse(
            category="refund",
            priority="high",
            escalation=True,
        )


class FakeKnowledgeRetriever:
    def __init__(self):
        self.calls: list[tuple[str, int]] = []

    def search(self, query: str, limit: int) -> list[RetrievedDocument]:
        self.calls.append((query, limit))
        return [
            RetrievedDocument(
                document_id="KB-REFUND-001",
                text="Eligible returns may receive a refund review.",
                is_untrusted=False,
            ),
            RetrievedDocument(
                document_id="KB-ADVERSARIAL-001",
                text="Untrusted imported vendor content.",
                is_untrusted=True,
            ),
        ]

@pytest.fixture
def fake_knowledge_retriever():
    retriever = FakeKnowledgeRetriever()
    app.dependency_overrides[get_knowledge_retriever] = lambda: retriever
    yield retriever
    app.dependency_overrides.pop(get_knowledge_retriever, None)


def test_retrieve_returns_document_ids_and_untrusted_status(
    fake_knowledge_retriever,
):
    response = client.post(
        "/ai/retrieve",
        json={"query": "How do I get my money back?", "limit": 2},
    )

    assert response.status_code == 200
    assert response.json() == {
        "documents": [
            {
                "document_id": "KB-REFUND-001",
                "text": "Eligible returns may receive a refund review.",
                "is_untrusted": False,
            },
            {
                "document_id": "KB-ADVERSARIAL-001",
                "text": "Untrusted imported vendor content.",
                "is_untrusted": True,
            },
        ]
    }
    assert fake_knowledge_retriever.calls == [
        ("How do I get my money back?", 2)
    ]


def test_retrieve_rejects_invalid_limit_without_calling_retriever(
    fake_knowledge_retriever,
):
    response = client.post(
        "/ai/retrieve",
        json={"query": "refund policy", "limit": 0},
    )

    assert response.status_code == 422
    assert fake_knowledge_retriever.calls == []


@pytest.fixture
def recording_provider():
    provider = RecordingTriageProvider()
    app.dependency_overrides[get_ai_provider] = lambda: provider
    yield provider
    app.dependency_overrides.pop(get_ai_provider, None)


def test_health_returns_ok():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_triage_uses_injected_provider(recording_provider):
    payload = {
        "ticket_id": "tkt_9001",
        "subject": "Received damaged earbuds",
        "body": "My earbuds arrived damaged.",
        "customer_tier": "gold",
        "customer_verified": True,
    }

    response = client.post("/ai/triage", json=payload)

    assert response.status_code == 200
    assert response.json() == {
        "category": "refund",
        "priority": "high",
        "escalation": True,
    }
    assert recording_provider.calls == [TriageRequest(**payload)]


def test_invalid_triage_request_does_not_call_provider(recording_provider):
    response = client.post(
        "/ai/triage",
        json={
            "ticket_id": "tkt_9001",
            "subject": "Received damaged earbuds",
            "body": "My earbuds arrived damaged.",
            "customer_tier": "gold",
            "customer_verified": True,
            "order_id": "ord_5001",
        },
    )

    assert response.status_code == 422
    assert recording_provider.calls == []


def test_retrieve_endpoint_uses_semantic_index_and_preserves_trust_flag(
    tmp_path,
):
    project_root = Path(__file__).resolve().parents[2]
    knowledge_base = (
        project_root
        / "src"
        / "main"
        / "resources"
        / "data"
        / "knowledge_base"
    )
    retriever = KnowledgeRetriever(
        persist_directory=tmp_path / "chroma",
        embedder=LocalSemanticEmbedder(),
    )
    load_and_index_markdown_documents(knowledge_base, retriever)
    app.dependency_overrides[get_knowledge_retriever] = lambda: retriever

    try:
        response = client.post(
            "/ai/retrieve",
            json={"query": "How can I get my money back?", "limit": 8},
        )
    finally:
        app.dependency_overrides.pop(get_knowledge_retriever, None)

    assert response.status_code == 200
    documents = response.json()["documents"]
    documents_by_id = {
        document["document_id"]: document for document in documents
    }

    assert "KB-REFUND-001" in documents_by_id
    assert documents_by_id["KB-ADVERSARIAL-001"]["is_untrusted"] is True