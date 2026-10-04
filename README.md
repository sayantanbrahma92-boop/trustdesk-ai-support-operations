# TrustDesk

TrustDesk is a support-operations application that helps agents review customer tickets with AI assistance. Agents can inspect ticket, customer, and order context; run AI triage; generate a knowledge-grounded draft with citations; and request a human-approved refund review.

The application uses a hybrid architecture: Spring Boot owns business data and sensitive actions, a Python service provides AI and knowledge retrieval, and a React frontend calls the Spring Boot API.

## Features

- Ticket inbox with customer and linked order context
- AI triage into shipping, refund, warranty, billing, account security, or general, with a priority and escalation decision
- Retrieval-augmented reply drafts with cited knowledge-base document IDs
- Refusal or escalation when evidence is insufficient or an application guardrail is triggered
- Local semantic retrieval with ChromaDB and local embeddings
- PostgreSQL persistence for tickets, customers, orders, approvals, refund reviews, AI drafts, and AI-run traces
- Caffeine cache for triage and draft responses, keyed by the full request and configured to expire after five minutes
- Demo-token-protected refund-review workflow with idempotency
- Evaluation runner and UI display of the latest saved report

## Architecture and workflow

```text
React support-agent UI
        |
        | REST/JSON
        v
Spring Boot application
  - ticket/customer/order APIs
  - PostgreSQL persistence
  - AI response cache
  - demo-token checks for approval routes
  - approval, idempotency, and sensitive-action enforcement
  - AI draft persistence and run traces
  - evaluation-report endpoint
        |
        | REST/JSON
        v
Python FastAPI AI service
  - mockable AI-provider interface and OpenRouter adapter
  - triage and draft workflows
  - application-level guardrails
  - local embeddings and ChromaDB retrieval
```

For triage, Spring loads the ticket context and sends it to Python. For a draft, Python retrieves relevant knowledge documents, excludes documents marked untrusted from policy evidence, requests a structured response from the AI provider, then validates support and citations against trusted retrieved documents.

The Python service may recommend or generate content, but it does not authorize or execute sensitive business actions. Spring creates a pending refund-review request and requires a human decision. Only approval causes Spring to persist the refund-review record and mark the action as executed. This demo records a refund review; it does not issue a payment refund or move money.

## Tools and technologies

| Area | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.1.1, Spring MVC |
| Persistence | PostgreSQL, Spring Data JPA |
| AI/RAG service | Python, FastAPI |
| AI provider | OpenAI-compatible client configured for OpenRouter |
| Retrieval | ChromaDB persistent local vector store and local semantic embeddings |
| Cache | Caffeine with bounded size and five-minute expiry |
| Frontend | React 19, Vite |
| Java tests | JUnit, Spring Boot test support, Mockito |
| Python tests | pytest, FastAPI TestClient |
| Frontend checks | Oxlint and Vite production build |

## Requirements

- JDK 25
- Node.js and npm
- Python 3.13 recommended
- PostgreSQL
- An OpenRouter API key for live AI calls and evaluation

## Configuration

Keep secrets in local environment variables or local configuration files. Do not commit them.

### OpenRouter

Create `ai-service\.env`:

```dotenv
OPENROUTER_API_KEY=your-local-key
OPENROUTER_MODEL=openai/gpt-5-mini
OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
```

The Python provider loads this file. Do not paste the key into source code or commit the file.

### PostgreSQL and Spring Boot

Spring Boot reads these optional environment variables:

- `DB_URL` — defaults to `jdbc:postgresql://localhost:5432/trustdesk`
- `DB_USERNAME` — database username for the local setup
- `DB_PASSWORD` — database password for the local setup
- `TRUSTDESK_DEMO_TOKEN` — required for approval and rejection endpoints
- `AI_SERVICE_BASE_URL` — defaults to `http://localhost:8001`

Create the PostgreSQL database and user before starting Spring Boot. The application currently uses JPA schema-update mode for this development project.

Generate a random demo token and save it as a Windows user environment variable. Example PowerShell:

```powershell
$bytes = New-Object 'byte[]' 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$token = [BitConverter]::ToString($bytes).Replace("-", "")
$rng.Dispose()
[Environment]::SetEnvironmentVariable("TRUSTDESK_DEMO_TOKEN", $token, "User")
```

The token must be available to the Spring Boot process and entered in the UI when requesting, approving, or rejecting a refund review.

### ChromaDB data directory

The Python service defaults to `ai-service\data\chroma`. To use another persistent local index directory, set `TRUSTDESK_CHROMA_PATH` before starting it:

```powershell
$env:TRUSTDESK_CHROMA_PATH = "C:\Project\trustdesk\ai-service\data\chroma-ui-test"
```

The index is generated from the Markdown knowledge-base documents. Chroma data is local generated data and should not be committed.

## Run locally

Run each service in a separate PowerShell window.

### 1. Python AI service

If the virtual environment has not been created yet:

```powershell
cd C:\Project\trustdesk\ai-service
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Start the service:

```powershell
cd C:\Project\trustdesk\ai-service
$env:TRUSTDESK_CHROMA_PATH = "C:\Project\trustdesk\ai-service\data\chroma-ui-test"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

### 2. Spring Boot backend

Set the AI-service URL and demo token in the same PowerShell window used to start Spring:

```powershell
cd C:\Project\trustdesk
$env:AI_SERVICE_BASE_URL = "http://127.0.0.1:8001"
$env:TRUSTDESK_DEMO_TOKEN = [Environment]::GetEnvironmentVariable("TRUSTDESK_DEMO_TOKEN", "User")
.\gradlew.bat bootRun
```

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in that window if your PostgreSQL connection differs from the configured defaults.

### 3. React frontend

```powershell
cd C:\Project\trustdesk\frontend
npm install
npm run dev
```

Open the Vite URL printed in the terminal, usually `http://localhost:5173/`. Vite proxies `/api` calls to Spring Boot at `http://localhost:8080`.

## API overview

The frontend calls the Spring Boot API. Spring Boot calls the Python service for AI functions.

### Spring Boot API

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/tickets` | List tickets with customer and order context |
| `GET` | `/api/tickets/{id}` | Fetch a ticket with customer and order context |
| `GET` | `/api/tickets/{id}/knowledge?limit=5` | Retrieve relevant knowledge documents |
| `POST` | `/api/tickets/{id}/triage` | Run ticket triage |
| `POST` | `/api/tickets/{id}/draft?limit=5` | Generate a cited draft or escalation result |
| `POST` | `/api/tickets/{ticketId}/refund-review` | Create or retrieve a pending refund-review approval |
| `POST` | `/api/approvals/{approvalId}/approve` | Approve a pending request and record the review |
| `POST` | `/api/approvals/{approvalId}/reject` | Reject a pending request |
| `GET` | `/api/evaluation/latest` | Read the latest saved evaluation report |

Approval and rejection endpoints require the `X-Demo-Token` header. The refund-review request also requires an `Idempotency-Key` header. Approval and rejection accept a reviewer label as JSON:

```json
{
  "reviewer": "demo-agent"
}
```

Example request to start a refund review:

```powershell
$headers = @{
    "X-Demo-Token" = $env:TRUSTDESK_DEMO_TOKEN
    "Idempotency-Key" = "refund-review-demo-001"
}
Invoke-RestMethod `
    -Uri "http://localhost:8080/api/tickets/tkt_9001/refund-review" `
    -Method Post `
    -Headers $headers
```

Use the returned approval `id` to approve or reject the request. Repeating a request with the same idempotency key and ticket returns the existing approval rather than creating a duplicate.

### Python AI service API

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/health` | Health check |
| `POST` | `/ai/triage` | Return category, priority, and escalation |
| `POST` | `/ai/retrieve` | Return matching documents and trust flags |
| `POST` | `/ai/draft` | Return a supported cited draft or escalation |

Example triage request:

```json
{
  "ticket_id": "tkt_9001",
  "subject": "Received damaged earbuds",
  "body": "My earbuds arrived damaged.",
  "customer_tier": "gold",
  "customer_verified": true
}
```

## Evaluation

The evaluation runner reads the supplied evaluation cases and ticket/customer fixtures, calls the AI workflow, computes metrics, and writes:

```text
ai-service\reports\evaluation-summary.json
```

Run it from the AI service directory:

```powershell
cd C:\Project\trustdesk\ai-service
.\.venv\Scripts\python.exe -m app.evaluation
```

The evaluator makes live AI-provider calls and overwrites the report by default. The Spring endpoint and React quality panel display the latest saved report; loading the panel does not start an evaluation.

The report includes triage category-and-priority accuracy, citation coverage, unsafe-action blocking, triage escalation accuracy, and draft-escalation accuracy. It currently covers the supplied eight cases; results are regression measurements for that dataset, not guarantees about unseen tickets.

## Tests and frontend checks

Java tests:

```powershell
cd C:\Project\trustdesk
.\gradlew.bat test
```

Python tests:

```powershell
cd C:\Project\trustdesk\ai-service
.\.venv\Scripts\python.exe -m pytest -q
```

Frontend lint and production build:

```powershell
cd C:\Project\trustdesk\frontend
npm run lint
npm run build
```

## Trust boundaries and limitations

- Customer messages and retrieved knowledge-base documents are untrusted input. Prompts tell the AI to treat them as data, while application checks enforce known guardrails. Prompt wording or XML-like formatting is not a security boundary.
- `KB-ADVERSARIAL-001` is intentionally unsafe, is marked untrusted, and must not be used as policy evidence or followed as an instruction.
- Spring Boot is authoritative for demo-token checks, approval decisions, idempotency, and sensitive action execution. The Python service recommends or generates responses only.
- The reviewer value is a submitted demo label, not an independently authenticated identity or role.
- The refund-review action records a review operation; it does not issue a refund payment.
- Regex guardrails provide defense-in-depth for known adversarial examples and are not a general-purpose prompt-injection detector.
- Draft citations are validated against trusted retrieved document IDs. Citation validity alone does not prove every sentence is relevant or sufficiently supported; agents should review drafts before sending.
- ChromaDB is a local retrieval index, not the business system of record.
- The evaluation dataset is finite and small; its scores do not guarantee model behavior on new cases.
