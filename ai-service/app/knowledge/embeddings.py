from chromadb.utils.embedding_functions import DefaultEmbeddingFunction


class LocalSemanticEmbedder:
    def __init__(self):
        self._embedding_function = DefaultEmbeddingFunction()

    def embed(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []

        vectors = self._embedding_function(texts)
        return [
            vector.tolist() if hasattr(vector, "tolist") else list(vector)
            for vector in vectors
        ]