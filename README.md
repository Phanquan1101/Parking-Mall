# ParkFlow Mall

ParkFlow Mall is a microservice-oriented smart parking and reservation management platform for shopping malls.

## Current implementation status

UI-04 - Final Demo Flow Polish and Visual QA is complete. The frontend now presents a coherent graduation-demo route from homepage through reservation, Gate Entry, customer QR ticket, merchant discount, payment simulation, Exit Pass, dashboard, and optional Offline Staff mode. Shared status labels, alerts, copy controls, responsive spacing, and QR/Exit Pass security messaging are presentation-only improvements; no API or business behavior changed.

Recommended demo script: **Homepage → Reservation → Gate Entry → Customer Ticket → Merchant Validation → Payment Simulation → Exit Pass → Dashboard → Offline Staff**. Use `/staff/gate-entry` as the primary live console, then validate/check-out through Postman or backend APIs where no staff UI exists.

UI-03 - Customer, Reservation, Merchant Pages Deep Polish is complete. The homepage, customer ticket, reservation create/detail, and merchant invoice validation pages now use clearer Vietnamese copy, mobile-friendly cards/forms, status feedback, and safe token/code presentation. Recommended demo flow: open the homepage, create a reservation, check in through Gate Entry, open the customer ticket, validate merchant invoices, simulate payment, then generate an Exit Pass.

UI-02 - Staff Operations Pages Deep Polish is complete. `/staff/gate-entry` is now the primary live-demo console, with clearer camera, recognition, manual-fallback, confirmation, and customer QR-result areas. `/dashboard`, `/staff/offline`, and `/staff/ocr-checkin` have more readable operational status, tables, empty states, and responsive layouts. This is visual-only work: existing JWT entry, API requests, camera/OCR behavior, and business rules are unchanged.

UI-01 - Frontend Design System and App Shell Revamp is complete. The React app now uses a lightweight custom design system, responsive staff/admin AppShell navigation, and a separate customer-facing public shell. Existing API calls and all established routes remain unchanged: `/`, `/tickets/:lookupToken`, `/reservations/new`, `/reservations/:reservationCode`, `/merchant/validate`, `/staff/offline`, `/staff/ocr-checkin`, `/staff/gate-entry`, and `/dashboard`.

Demo navigation: use the public homepage for customer ticket/reservation entry; use the sidebar on staff/admin pages for Dashboard, Gate Entry, OCR Upload, Offline Sync, Merchant Invoice Validation, and Reservation. The UI keeps demo JWT entry on the existing staff tools; it does not introduce a new login flow.

Slice 11A - Read-only Operational Dashboard is complete. Staff/admin can use an existing JWT to view client-side summaries of parking sessions and reservations; reconciliation details remain ADMIN-only.

Next slice: Slice 11B - Dashboard Polish and Demo Readiness.

Slice 10B - Real Gemini OCR Provider is complete. Vision Service selects `DEMO_OCR` (the default filename-based fallback) or `GEMINI` through `VISION_OCR_PROVIDER`. To use Gemini, set `GEMINI_API_KEY` and optionally `GEMINI_MODEL=gemini-2.5-flash` only in the Vision Service environment. The browser never receives the API key; all Gemini calls stay server-side. OCR remains a suggestion that staff must confirm or correct before a normal check-in.

Slice 10C - Live Camera Gate Entry is complete. `/staff/gate-entry` keeps a browser camera preview open, sends throttled transient frames to the existing Vision endpoint, then pauses on a confident plate suggestion for staff confirmation. For real camera recognition use `VISION_OCR_PROVIDER=GEMINI` with a server-side `GEMINI_API_KEY`; `DEMO_OCR` remains useful for the filename/mock upload flow.

Slice 10D - Gate Entry Stability is complete. The camera flow has a local 15-second same-plate cooldown, single-submit check-in guard, capped OCR retry backoff, camera disconnect handling, and a manual check-in fallback. These are UX safeguards only: Parking Service remains the final duplicate-active-plate authority.

Slice 8 admin demo: log in as `admin`, then call `POST /api/payments/reconciliation/run` and inspect `GET /api/payments/reconciliation/items`. Reconciliation is in-memory only; it uses no SePay or real banking and never refunds, creates an Exit Pass, or checks out a vehicle.

Merchant demo: open `http://localhost:5173/merchant/validate`, paste a Merchant/Admin JWT, enter the QR Lookup Token plus invoice code/amount. The demo rule applies a `5000` discount when aggregate eligible invoices reach `300000`; invoice codes are globally single-use.

Reservation demo: open `http://localhost:5173/reservations/new`, create a reservation, and copy its opaque code. A staff user includes `reservationCode` in the normal parking check-in request. Parking consumes the reservation over its internal service route, then creates a normal `UNPAID` Parking Session. Slice 9 has no reservation payment or deposit.

OCR demo: open `http://localhost:5173/staff/ocr-checkin`, paste a staff/admin JWT, upload a JPEG/PNG/WebP image, review the provider and candidate, then confirm or correct the plate before submitting check-in. OCR never auto-creates a session or authorizes a gate; images are not stored.

Live Gate Entry demo: start Identity, Gateway, Parking, Vision, and Web; configure Vision with `DEMO_OCR` for filename/mock testing or `GEMINI` plus `GEMINI_API_KEY` for real camera images. Log in as `staff`, open `http://localhost:5173/staff/gate-entry`, paste the JWT, start the camera, point a plate at the guide, review or edit the locked candidate, and press Enter to confirm check-in. Show the generated customer ticket link, then select **Next Vehicle** to resume scanning. Staff confirmation remains required; the displayed QR Lookup Ticket never authorizes exit.

Gate Entry stability notes: use a real or clearly printed plate and keep it steady inside the guide until the candidate locks; press Enter only once after reviewing it. If the same plate remains in view after **Next Vehicle**, the local cooldown explains why it is not locked again; use **Clear cooldown** only for an intentional override. Use **Nhập biển số thủ công** when camera/OCR is unavailable. If the browser blocks the camera, grant camera permission and restart it; if Vision reports configuration unavailable, verify the server-side Gemini configuration. Camera frames are never stored.

Dashboard demo: log in as `admin` or `staff`, open `http://localhost:5173/dashboard`, paste the JWT, then refresh. The dashboard is read-only and aggregates existing parking/reservation APIs in the browser. Payment reconciliation details load only for `ADMIN`; staff see an ADMIN-only notice.

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

Manual Slice 5 demo flow:

1. Log in as `staff` and create a parking session.
2. Open its customer ticket, create a payment order, then simulate payment success.
3. Generate the Exit Pass from the ticket page after it shows `PAID`.
4. As staff, validate the pass and matching plate at `POST /api/parking/exit-passes/{exitPassToken}/validate`.
5. Check out the same session at `POST /api/parking/sessions/{sessionId}/check-out`.
6. Confirm the ticket/session status is `EXITED`; reusing the pass is rejected.

QR Lookup Token cannot authorize exit. Dynamic Exit Pass is opaque, short-lived (60 seconds by default), and one-time use. Staff manual override still requires payment verification and a reason.

Manual Slice 6 offline demo flow: log in as `staff`, open `http://localhost:5173/staff/offline`, paste the JWT, add an offline check-in while offline, then return online and sync. The server returns an official session ID/code for `SYNCED`, and a same-plate event becomes `CONFLICT`.

Only offline check-in is supported in Slice 6. Offline check-out never automatically exits a vehicle, and rejected/conflicted queue entries remain visible for staff review.

Run the vision placeholder:

```powershell
cd services/vision-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8090
```

## Documentation source of truth

## UAT and Demo

- [UAT Checklist](docs/UAT_CHECKLIST.md) — 99 ca kiểm thử chấp nhận thủ công, bao gồm UI và Postman/API.
- [UAT Execution Run 1](docs/UAT_EXECUTION_RUN_1.md) — execution sheet đang dùng để tester ghi kết quả, evidence và bug note cho Run 1.
- [Demo Script](docs/DEMO_SCRIPT.md) — kịch bản trình bày end-to-end trong 10–15 phút.

Read `AGENTS.md` first, then consult:

- `docs/BRD.md`, `docs/PRD.md`, and `docs/SAD.md`
- `docs/BUSINESS_RULES.md`, `docs/API_CONTRACT.md`, and `docs/DATABASE_SCHEMA.md`
- `docs/TEST_MATRIX.md`, `docs/NFR.md`, and `docs/DECISION_LOG.md`
- `agent/PLAN.md`, `agent/TASKS.md`, and `agent/VERTICAL_SLICE_ROADMAP.md`

Warning: `docs-human/` is for PDF, Word, and other human-submission files only. It is not an implementation source of truth unless reflected into `docs/`.
