import re
from pathlib import Path

from app.knowledge.retrieval import KnowledgeDocument, KnowledgeRetriever


_DOCUMENT_ID_PATTERN = re.compile(r"^Doc ID:\s*(\S+)\s*$", re.MULTILINE)


def load_markdown_documents(directory: Path) -> list[KnowledgeDocument]:
    if not directory.is_dir():
        raise FileNotFoundError(f"Knowledge-base directory not found: {directory}")

    paths = sorted(directory.glob("*.md"))
    if not paths:
        raise FileNotFoundError(f"No Markdown documents found in: {directory}")

    documents = []
    seen_ids = set()

    for path in paths:
        text = path.read_text(encoding="utf-8")
        match = _DOCUMENT_ID_PATTERN.search(text)

        if match is None:
            raise ValueError(f"Missing Doc ID in Markdown file: {path.name}")

        document_id = match.group(1)
        if document_id in seen_ids:
            raise ValueError(f"Duplicate Doc ID {document_id} in: {path.name}")

        seen_ids.add(document_id)
        documents.append(KnowledgeDocument(id=document_id, text=text))

    return documents

def load_and_index_markdown_documents(
    directory: Path,
    retriever: KnowledgeRetriever,
) -> list[str]:
    documents = load_markdown_documents(directory)
    retriever.add_documents(documents)
    return [document.id for document in documents]