from pathlib import Path

import pytest

from app.kb_loader import (
    load_and_index_markdown_documents,
    load_markdown_documents,
)
from app.retrieval import KnowledgeRetriever




class KeywordTestEmbedder:
    def embed(self, texts: list[str]) -> list[list[float]]:
        return [
            [
                float("refund" in text.lower()),
                float("shipping" in text.lower()),
            ]
            for text in texts
        ]


def test_loads_all_provided_documents_and_preserves_ids():
    knowledge_base = (
        Path(__file__).resolve().parents[2]
        / "src"
        / "main"
        / "resources"
        / "data"
        / "knowledge_base"
    )

    documents = load_markdown_documents(knowledge_base)

    assert {document.id for document in documents} == {
        "KB-ADVERSARIAL-001",
        "KB-ACCOUNT-001",
        "KB-BILLING-001",
        "KB-COUPON-001",
        "KB-REFUND-001",
        "KB-SHIPPING-001",
        "KB-SECURITY-001",
        "KB-WARRANTY-001",
    }
    assert all(document.text for document in documents)


def test_rejects_markdown_without_document_id(tmp_path):
    (tmp_path / "missing-id.md").write_text(
        "# Policy\nNo document ID here.\n",
        encoding="utf-8",
    )

    with pytest.raises(ValueError, match="missing-id.md"):
        load_markdown_documents(tmp_path)


def test_indexes_all_kb_documents_and_preserves_adversarial_flag(tmp_path):
    knowledge_base = (
        Path(__file__).resolve().parents[2]
        / "src"
        / "main"
        / "resources"
        / "data"
        / "knowledge_base"
    )
    retriever = KnowledgeRetriever(
        persist_directory=tmp_path / "chroma",
        embedder=KeywordTestEmbedder(),
    )

    indexed_ids = load_and_index_markdown_documents(knowledge_base, retriever)
    results = retriever.search("refund", limit=8)
    results_by_id = {result.document_id: result for result in results}

    assert len(indexed_ids) == 8
    assert "KB-REFUND-001" in indexed_ids
    assert "KB-ADVERSARIAL-001" in indexed_ids
    assert results_by_id["KB-ADVERSARIAL-001"].is_untrusted is True