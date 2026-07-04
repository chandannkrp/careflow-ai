import json
from typing import Any

from openai import OpenAI

from app.config import Settings
from app.models import ChatRequest, PatientMatch


SYSTEM_INSTRUCTION = """
You are CareFlow AI's staff operations assistant.
Answer administrative questions about patient lookup, intake completeness, queue context,
doctor or bed allocation context, wait-time visibility, and workflow next steps.
Do not diagnose, prescribe, or recommend clinical treatment.
If asked for clinical care, redirect to a licensed clinician and provide only operational facts.
Use matched patient context when relevant. Keep answers concise and return plain text.
"""


class OpenAIGateway:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.client = OpenAI(api_key=settings.openai_api_key) if settings.openai_enabled else None

    @property
    def enabled(self) -> bool:
        return self.client is not None

    def embed(self, text: str) -> list[float]:
        if not self.client:
            return []
        response = self.client.embeddings.create(
            model=self.settings.openai_embedding_model,
            input=text,
        )
        return list(response.data[0].embedding)

    def respond(self, request: ChatRequest, matches: list[PatientMatch]) -> str:
        if not self.client:
            return ''
        context: dict[str, Any] = {
            'actor': {
                'name': request.actor_name or 'Unknown',
                'role': request.actor_role or 'Unknown',
            },
            'matchedPatients': [match.model_dump(by_alias=True) for match in matches],
            'operationalContext': request.context,
        }
        response = self.client.responses.create(
            model=self.settings.openai_chat_model,
            input=[
                {'role': 'developer', 'content': SYSTEM_INSTRUCTION},
                {'role': 'user', 'content': json.dumps({'message': request.message, 'context': context})},
            ],
        )
        output_text = getattr(response, 'output_text', None)
        if output_text:
            return output_text.strip()
        return ''
