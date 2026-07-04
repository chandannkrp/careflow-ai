from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.amqp_consumer import PatientAmqpConsumer
from app.config import get_settings
from app.models import ChatRequest, ChatResponse, HealthResponse, PatientDetails, PatientIngestRequest, PatientMatch
from app.openai_gateway import OpenAIGateway
from app.vector_store import PatientVectorStore

settings = get_settings()
openai_gateway = OpenAIGateway(settings)
vector_store = PatientVectorStore(settings.vector_store_path, openai_gateway.embed)
amqp_consumer = PatientAmqpConsumer(settings, vector_store)


@asynccontextmanager
async def lifespan(_app: FastAPI):
    amqp_consumer.start()
    yield


app = FastAPI(title='CareFlow AI Service', version='0.1.0', lifespan=lifespan)


@app.get('/health', response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status='ok',
        openaiEnabled=openai_gateway.enabled,
        amqpEnabled=settings.amqp_enabled,
        indexedPatients=vector_store.count(),
    )


@app.post('/patients/ingest', response_model=PatientMatch)
def ingest_patient(request: PatientIngestRequest) -> PatientMatch:
    return vector_store.upsert(request.patient)


@app.post('/patients/search', response_model=list[PatientMatch])
def search_patients(request: ChatRequest) -> list[PatientMatch]:
    return vector_store.search(request.message, limit=5)


@app.post('/chat', response_model=ChatResponse)
def chat(request: ChatRequest) -> ChatResponse:
    matches = vector_store.search(request.message, limit=5)
    suggested_actions = suggest_actions(request.message)

    if openai_gateway.enabled:
        answer = openai_gateway.respond(request, matches)
        if answer:
            return ChatResponse(
                message=answer,
                suggestedActions=suggested_actions,
                aiBacked=True,
                matchedPatients=matches,
            )

    return ChatResponse(
        message=fallback_answer(request.message, matches),
        suggestedActions=suggested_actions,
        aiBacked=False,
        matchedPatients=matches,
    )


def fallback_answer(message: str, matches: list[PatientMatch]) -> str:
    normalized = message.lower()
    if 'patient' in normalized or 'booking' in normalized or matches:
        if not matches:
            return 'I could not find a matching patient in the AI service vector store yet.'
        summaries = '; '.join(
            f'{match.patient_display_id or match.patient_id}: {match.summary}'
            for match in matches[:3]
        )
        return f'Closest patient matches: {summaries}'
    if 'intake' in normalized:
        return 'For intake workflow questions I can help with required fields, completeness checks, and patient lookup once records are indexed.'
    if 'urgent' in normalized or 'critical' in normalized:
        return 'I can look up urgent or critical patients once their details have been ingested into the AI service.'
    if 'doctor' in normalized or 'bed' in normalized or 'queue' in normalized:
        return 'I can answer allocation and queue questions when the backend sends context or matching patient records are indexed.'
    return 'I can answer general workflow questions with OpenAI configured, and patient questions after AMQP ingestion has indexed patient details.'


def suggest_actions(message: str) -> list[str]:
    normalized = message.lower()
    if 'refresh' in normalized or 'reload' in normalized:
        return ['refresh_queue', 'refresh_dashboard']
    if 'critical' in normalized or 'urgent' in normalized:
        return ['filter_critical_high']
    if 'intake' in normalized:
        return ['open_intake']
    return []


def build_patient_details(payload: dict) -> PatientDetails:
    return PatientDetails.model_validate(payload)
