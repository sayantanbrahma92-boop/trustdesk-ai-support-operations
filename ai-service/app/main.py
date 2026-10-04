from functools import lru_cache
from pathlib import Path

from fastapi import Depends, FastAPI

from app.embeddings import LocalSemanticEmbedder
from app.kb_loader import load_and_index_markdown_documents
from app.providers import AIProvider, get_ai_provider
from app.retrieval import KnowledgeRetriever
from app.schemas import (
    DraftRequest,
    DraftResponse,
    RetrievalRequest,
    RetrievalResponse,
    RetrievedDocumentResponse,
    TriageRequest,
    TriageResponse,
)
from app.workflow import AIWorkflow

app = FastAPI(title="TrustDesk AI Service")


@lru_cache
def get_knowledge_retriever() -> KnowledgeRetriever:
    project_root = Path(__file__).resolve().parents[2]
    knowledge_base = (
        project_root
        / "src"
        / "main"
        / "resources"
        / "data"
        / "knowledge_base"
    )

    retriever = KnowledgeRetriever(embedder=LocalSemanticEmbedder())
    load_and_index_markdown_documents(knowledge_base, retriever)
    return retriever


def get_ai_workflow(
    provider: AIProvider = Depends(get_ai_provider),
    retriever: KnowledgeRetriever = Depends(get_knowledge_retriever),
) -> AIWorkflow:
    return AIWorkflow(provider, retriever)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/ai/triage", response_model=TriageResponse)
def triage(
    request: TriageRequest,
    workflow: AIWorkflow = Depends(get_ai_workflow),
) -> TriageResponse:
    return workflow.triage(request)


@app.post("/ai/retrieve", response_model=RetrievalResponse)
def retrieve(
    request: RetrievalRequest,
    retriever: KnowledgeRetriever = Depends(get_knowledge_retriever),
) -> RetrievalResponse:
    documents = retriever.search(request.query, limit=request.limit)
    return RetrievalResponse(
        documents=[
            RetrievedDocumentResponse(
                document_id=document.document_id,
                text=document.text,
                is_untrusted=document.is_untrusted,
            )
            for document in documents
        ]
    )


@app.post("/ai/draft", response_model=DraftResponse)
def draft(
    request: DraftRequest,
    workflow: AIWorkflow = Depends(get_ai_workflow),
) -> DraftResponse:
    return workflow.draft(request)