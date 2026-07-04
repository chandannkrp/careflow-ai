from __future__ import annotations

import hashlib
import json
import math
import threading
from pathlib import Path
from typing import Any, Callable

import numpy as np

from app.models import PatientDetails, PatientMatch

EmbeddingFunction = Callable[[str], list[float]]


class PatientVectorStore:
    def __init__(self, path: str, embed: EmbeddingFunction, fallback_dimensions: int = 384) -> None:
        self.path = Path(path)
        self.embed = embed
        self.fallback_dimensions = fallback_dimensions
        self._lock = threading.Lock()
        self._documents: dict[str, dict[str, Any]] = {}
        self._load()

    def count(self) -> int:
        with self._lock:
            return len(self._documents)

    def upsert(self, patient: PatientDetails) -> PatientMatch:
        text = patient.searchable_text()
        vector = self._safe_embed(text)
        payload = patient.model_dump(by_alias=True)
        payload['raw'] = patient.raw or payload.get('raw') or {}
        document = {
            'id': patient.document_id,
            'text': text,
            'embedding': vector,
            'patient': payload,
        }
        with self._lock:
            self._documents[patient.document_id] = document
            self._persist()
        return self._to_match(document, 1.0)

    def search(self, query: str, limit: int = 5) -> list[PatientMatch]:
        query_vector = self._safe_embed(query)
        with self._lock:
            documents = list(self._documents.values())
        ranked = [
            self._to_match(document, self._cosine(query_vector, document['embedding']))
            for document in documents
        ]
        return [
            match for match in sorted(ranked, key=lambda item: item.score, reverse=True)
            if match.score > 0
        ][:limit]

    def _safe_embed(self, text: str) -> list[float]:
        try:
            vector = self.embed(text)
        except Exception:
            vector = []
        if not vector:
            return self._hash_embedding(text)
        return vector

    def _hash_embedding(self, text: str) -> list[float]:
        vector = [0.0] * self.fallback_dimensions
        for token in text.lower().split():
            digest = hashlib.sha256(token.encode('utf-8')).digest()
            index = int.from_bytes(digest[:4], 'big') % self.fallback_dimensions
            vector[index] += 1.0
        norm = math.sqrt(sum(value * value for value in vector))
        if norm == 0:
            return vector
        return [value / norm for value in vector]

    def _cosine(self, first: list[float], second: list[float]) -> float:
        if not first or not second:
            return 0.0
        length = min(len(first), len(second))
        first_array = np.array(first[:length], dtype=float)
        second_array = np.array(second[:length], dtype=float)
        denominator = np.linalg.norm(first_array) * np.linalg.norm(second_array)
        if denominator == 0:
            return 0.0
        return float(np.dot(first_array, second_array) / denominator)

    def _to_match(self, document: dict[str, Any], score: float) -> PatientMatch:
        patient = document['patient']
        return PatientMatch(
            patientDisplayId=patient.get('patientDisplayId'),
            patientId=patient.get('patientId'),
            intakeId=patient.get('intakeId'),
            score=round(score, 4),
            summary=self._summary(document['text']),
            details=patient,
        )

    def _summary(self, text: str) -> str:
        compact = ' '.join(text.split())
        return compact if len(compact) <= 240 else f'{compact[:237]}...'

    def _load(self) -> None:
        if not self.path.exists():
            return
        with self.path.open('r', encoding='utf-8') as file:
            payload = json.load(file)
        self._documents = {
            item['id']: item for item in payload.get('documents', [])
            if isinstance(item, dict) and item.get('id')
        }

    def _persist(self) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        payload = {'documents': list(self._documents.values())}
        with self.path.open('w', encoding='utf-8') as file:
            json.dump(payload, file, indent=2)
