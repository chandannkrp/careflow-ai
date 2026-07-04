# CareFlow AI Service

Python service for CareFlow conversational AI and patient-detail vector search.

## What It Does

- Consumes patient detail JSON from AMQP when `AMQP_URL` is configured.
- Stores patient embeddings in a simple local JSON vector store.
- Exposes `POST /chat` for the Spring backend chat proxy.
- Falls back to deterministic hash embeddings and simple local answers when OpenAI is not configured.
- Includes `openai_connection_check.py` for a standalone OpenAI smoke test.

## Setup

```powershell
cd ai-service
python -m venv .venv
. .venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
```

Keep `AMQP_URL=` empty until the broker URL is ready.

## Run

```powershell
uvicorn app.main:app --host 127.0.0.1 --port 8090
```

Then set the Spring backend environment variable when you want chat routed to this service:

```properties
AI_SERVICE_URL=http://127.0.0.1:8090
```

Leave `AI_SERVICE_URL=` empty to keep the current Spring fallback behavior.

## AMQP Message Shape

The consumer accepts either a raw patient object or an envelope:

```json
{
  "patient": {
    "patientId": "uuid",
    "intakeId": "uuid",
    "patientDisplayId": "CF-0001",
    "ageBand": "ADULT",
    "chiefComplaint": "Chest pain",
    "structuredSymptoms": ["chest pain", "shortness of breath"],
    "department": "Emergency",
    "assessment": {
      "finalCategory": "HIGH",
      "finalScore": 82,
      "scoreFactors": ["Chest pain", "Breathing difficulty"]
    }
  }
}
```

## OpenAI Connection Check

```powershell
cd ai-service
python openai_connection_check.py
```
