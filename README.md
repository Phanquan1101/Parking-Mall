# ParkFlow Mall

ParkFlow Mall is a microservice-oriented smart parking and reservation management platform for shopping malls.

## Current implementation status

Slice 2 - Parking Session + QR Lookup is complete. Parking Staff and Admin can create in-memory ACTIVE sessions, receive opaque lookup tokens, and expose public ticket lookup. Payment, exit authorization, and all later parking features remain unimplemented.

Next slice: Slice 3 - Customer Ticket Page.

## Tech stack

- Java 21, Spring Boot, Maven, and Actuator for Java service skeletons
- Python 3.11+, FastAPI, and Uvicorn for the Vision/OCR skeleton
- React, Vite, and TypeScript for the web skeleton
- Docker Compose for local orchestration planning
- Supabase PostgreSQL and Storage planned for later slices only

## Repository structure

```text
apps/web/                     React + Vite placeholder
services/api-gateway/         Spring Boot gateway skeleton
services/identity-service/    Spring Boot identity skeleton
services/parking-service/     Spring Boot parking skeleton
services/payment-service/     Spring Boot payment skeleton
services/merchant-service/    Spring Boot merchant skeleton
services/reservation-service/ Spring Boot reservation skeleton
services/reporting-service/   Spring Boot reporting skeleton
services/vision-service/      FastAPI health skeleton
packages/shared-contracts/    Future OpenAPI/type placeholder
docs/                         Canonical product, architecture, and contract docs
docs-human/                   PDF/Word human-submission artifacts only
agent/                        Slice plan, tasks, coding rules, and definition of done
infra/                        Future local, Railway, Supabase, and script assets
tests/                        Future cross-service test areas
```

## Local development

Copy `.env.example` to `.env` and set only local, non-production values when later slices need them. Slice 0 does not connect to Supabase, SePay, or OCR providers.

Run one Java service, for example:

```powershell
cd services/api-gateway
mvn spring-boot:run
```

Then visit `http://localhost:8080/actuator/health`.

For Slice 2, run Identity Service first, then Parking Service and API Gateway:

```powershell
cd services/identity-service
mvn spring-boot:run
```

```powershell
cd services/api-gateway
mvn spring-boot:run
```

```powershell
cd services/parking-service
mvn spring-boot:run
```

Demo credentials are in-memory only and must not be used outside local development:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `staff` | `staff123` | `PARKING_STAFF` |
| `merchant` | `merchant123` | `MERCHANT_STAFF` |

Create an in-memory session through the gateway after logging in as `staff`:

```powershell
curl.exe -X POST "http://localhost:8080/api/parking/sessions/check-in" `
  -H "Authorization: Bearer <accessToken>" `
  -H "Content-Type: application/json" `
  -d '{"vehiclePlate":"59A1-12345","vehicleType":"MOTORBIKE","entryGate":"GATE_IN_01","staffId":"staff-demo-id","plateSource":"MANUAL"}'
```

Open the returned `ticketUrl`, or use:

```powershell
curl.exe "http://localhost:8080/api/public/tickets/<qrLookupToken>"
```

Parking sessions are in-memory only for Slice 2 and disappear when Parking Service restarts.

Run the web placeholder:

```powershell
cd apps/web
npm install
npm run dev
```

Run the vision placeholder:

```powershell
cd services/vision-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8090
```

## Documentation source of truth

Read `AGENTS.md` first, then consult:

- `docs/BRD.md`, `docs/PRD.md`, and `docs/SAD.md`
- `docs/BUSINESS_RULES.md`, `docs/API_CONTRACT.md`, and `docs/DATABASE_SCHEMA.md`
- `docs/TEST_MATRIX.md`, `docs/NFR.md`, and `docs/DECISION_LOG.md`
- `agent/PLAN.md`, `agent/TASKS.md`, and `agent/VERTICAL_SLICE_ROADMAP.md`

Warning: `docs-human/` is for PDF, Word, and other human-submission files only. It is not an implementation source of truth unless reflected into `docs/`.
