# ParkFlow Mall

ParkFlow Mall is a microservice-oriented smart parking and reservation management platform for shopping malls.

## Current implementation status

Slice 4 - Payment Simulation is complete. Customers can create an in-memory payment order from the public ticket page and simulate payment success; Parking then reflects `PAID`. No real money is used and SePay Live is disabled.

Next slice: Slice 5 - Dynamic Exit Pass + Check-out.

## Tech stack

- Java 21, Spring Boot, Maven, and Actuator for Java service skeletons
- Python 3.11+, FastAPI, and Uvicorn for the Vision/OCR skeleton
- React, Vite, and TypeScript for the web skeleton
- Docker Compose for local orchestration planning
- Supabase PostgreSQL and Storage planned for later slices only

## Repository structure

```text
apps/web/                     React + Vite customer ticket page
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

## Local Development Modes

### Mode A — Hybrid Local Development (recommended)

Infrastructure runs in Docker while Spring Boot services run from IntelliJ IDEA and the React/Vite app runs from VS Code. Start PostgreSQL only:

```powershell
docker compose --profile infra up -d
```

Run these services directly in IntelliJ: api-gateway on `8080`, identity-service on `8081`, parking-service on `8082`, and payment-service on `8083`. Future merchant, reservation, and reporting services use `8084`, `8085`, and `8086`; Vision/OCR may use `8090`.

Run the frontend from VS Code:

```powershell
cd apps/web
npm install
npm run dev
```

Access the frontend at `http://localhost:5173`, gateway at `http://localhost:8080`, and PostgreSQL at `localhost:5432`. In this mode, service-to-service URLs use `http://localhost:8081`, `http://localhost:8082`, and `http://localhost:8083`, and persistence uses `jdbc:postgresql://localhost:5432/parkflow` when enabled.

### Mode B — Full Docker Stack (optional)

Stop IntelliJ backend services, or leave them running because Docker publishes a separate host-port range. Start the full backend stack with:

```powershell
docker compose --profile full-stack up --build
```

Access the Docker gateway at `http://localhost:18080`, identity at `http://localhost:18081`, parking at `http://localhost:18082`, and payment at `http://localhost:18083`. Set `VITE_API_BASE_URL=http://localhost:18080` in an uncommitted `apps/web/.env.local` when using the Docker backend. Stop it with:

```powershell
docker compose --profile full-stack down
```

Docker services communicate through internal names such as `http://parking-service:8082`. Do not point a locally running IntelliJ service to Docker hostnames; use `localhost` URLs instead.

Plain `docker compose up` starts no profiled services. Use the explicit `infra` or `full-stack` command above.

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

```powershell
cd services/payment-service
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

Run the customer ticket page:

```powershell
cd apps/web
npm install
npm run dev
```

The frontend calls `http://localhost:8080` by default. To use another gateway URL, copy `apps/web/.env.example` to `apps/web/.env` and set `VITE_API_BASE_URL`.

Open a returned lookup token in the customer ticket page:

```text
http://localhost:5173/tickets/<qrLookupToken>
```

Manual Slice 3 demo flow:

1. Start Identity Service, Parking Service, API Gateway, and the web app.
2. Log in as `staff` and create a parking session through the gateway.
3. Copy `qrLookupToken` from the check-in response.
4. Open `http://localhost:5173/tickets/<qrLookupToken>`.
5. Confirm the summary renders and an invalid token shows the safe not-found message.

For Slice 4, create a payment order on that ticket page, simulate success, and confirm the ticket changes to `PAID`. Simulation Mode uses no real money; SePay Live remains disabled.

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
