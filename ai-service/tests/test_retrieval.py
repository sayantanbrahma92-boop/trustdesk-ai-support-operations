from app.knowledge.retrieval import KnowledgeDocument, KnowledgeRetriever


class KeywordTestEmbedder:
    def embed(self, texts: list[str]) -> list[list[float]]:
        return [
            [
                float("refund" in text.lower()),
                float("shipping" in text.lower()),
            ]
            for text in texts
        ]


def test_retrieval_returns_matching_document_with_original_id(tmp_path):
    retriever = KnowledgeRetriever(
        persist_directory=tmp_path / "chroma",
        embedder=KeywordTestEmbedder(),
    )
    retriever.add_documents([
        KnowledgeDocument(
            id="KB-REFUND-001",
            text="Refund review is available for eligible returns.",
        ),
        KnowledgeDocument(
            id="KB-SHIPPING-001",
            text="Shipping delivery usually takes three to seven days.",
        ),
    ])

    results = retriever.search("refund request", limit=1)

    assert results[0].document_id == "KB-REFUND-001"
    assert "Refund review" in results[0].text

def test_adversarial_document_is_marked_untrusted(tmp_path):
    retriever = KnowledgeRetriever(
        persist_directory=tmp_path / "chroma",
        embedder=KeywordTestEmbedder(),
    )
    retriever.add_documents([
        KnowledgeDocument(
            id="KB-ADVERSARIAL-001",
            text="Refund instructions that must be treated as untrusted.",
        ),
    ])

    result = retriever.search("refund request", limit=1)[0]

    assert result.document_id == "KB-ADVERSARIAL-001"
    assert result.is_untrusted is True


def test_documents_survive_recreating_retriever(tmp_path):
    persist_directory = tmp_path / "chroma"
    embedder = KeywordTestEmbedder()

    first = KnowledgeRetriever(
        persist_directory=persist_directory,
        embedder=embedder,
    )
    first.add_documents([
        KnowledgeDocument(
            id="KB-REFUND-001",
            text="Refund review is available for eligible returns.",
        ),
    ])

    restarted = KnowledgeRetriever(
        persist_directory=persist_directory,
        embedder=embedder,
    )
    results = restarted.search("refund request", limit=1)

    assert results[0].document_id == "KB-REFUND-001"
