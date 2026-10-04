from pathlib import Path

from app.embeddings import LocalSemanticEmbedder
from app.kb_loader import load_and_index_markdown_documents
from app.retrieval import KnowledgeRetriever


def test_local_semantic_search_retrieves_refund_policy(tmp_path):
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
        embedder=LocalSemanticEmbedder(),
    )

    load_and_index_markdown_documents(knowledge_base, retriever)
    results = retriever.search(
        "A product arrived damaged. How can I get my money back?",
        limit=3,
    )

    assert "KB-REFUND-001" in {
        result.document_id for result in results
    }