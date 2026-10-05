from dataclasses import dataclass
from pathlib import Path
from typing import Protocol

import chromadb
import os



DEFAULT_PERSIST_DIRECTORY = Path(__file__).resolve().parents[1] / "data" / "chroma"

class Embedder(Protocol):
    def embed(self, texts: list[str]) -> list[list[float]]:
        ...


@dataclass(frozen=True)
class KnowledgeDocument:
    id: str
    text: str


@dataclass(frozen=True)
class RetrievedDocument:
    document_id: str
    text: str
    is_untrusted: bool


class KnowledgeRetriever:
    def __init__(
        self,
        embedder: Embedder,
        persist_directory: Path | None = None,
    ):
        configured_path = os.getenv("TRUSTDESK_CHROMA_PATH")
        storage_path = persist_directory or (
            Path(configured_path) if configured_path else DEFAULT_PERSIST_DIRECTORY
        )

        self._embedder = embedder
        client = chromadb.PersistentClient(path=str(storage_path))
        self._collection = client.get_or_create_collection(
            name="trustdesk_knowledge_semantic_v1",
            embedding_function=None,
            metadata={"hnsw:space": "cosine"},
        )

    def add_documents(self, documents: list[KnowledgeDocument]) -> None:
        if not documents:
            return

        self._collection.upsert(
            ids=[document.id for document in documents],
            documents=[document.text for document in documents],
            embeddings=self._embedder.embed([document.text for document in documents]),
            metadatas=[
                {
                    "document_id": document.id,
                    "is_untrusted": document.id == "KB-ADVERSARIAL-001",
                }
                for document in documents
            ],
        )

    def search(self, query: str, limit: int = 5) -> list[RetrievedDocument]:
        if limit < 1:
            raise ValueError("limit must be at least 1")

        matches = self._collection.query(
            query_embeddings=self._embedder.embed([query]),
            n_results=limit,
            include=["documents", "metadatas"],
        )

        ids = matches["ids"][0]
        texts = matches["documents"][0]
        metadatas = matches["metadatas"][0]

        return [
            RetrievedDocument(
                document_id=metadata["document_id"],
                text=text,
                is_untrusted=metadata["is_untrusted"],
            )
            for text, metadata in zip(texts, metadatas)
            if text is not None and metadata is not None
        ]